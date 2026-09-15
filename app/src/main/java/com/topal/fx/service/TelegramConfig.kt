package com.topal.fx.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

/**
 * Built-in configuration, anti-spam throttling, deduplication, and sequential
 * message queue dispatcher to strictly protect Telegram Bot from rate limits and bans.
 */
object TelegramConfig {
    const val BOT_TOKEN: String = "8921837204:AAF0QB1IIYMQmoW6lXLtfU6U0hH-lmk2GRA"
    const val CHAT_ID: String = "969346296"

    // Safe delay between consecutive messages to respect Telegram's 1 msg/sec limit
    private const val MIN_INTERVAL_MS = 1600L

    // Deduplication window in milliseconds (identical notifications within 45s are ignored)
    private const val DEDUP_WINDOW_MS = 45_000L

    // Message channel queue to serialize sending
    private val messageQueue = Channel<String>(capacity = 50)

    // LRU-style cache for deduplication: key -> timestamp
    private val recentHashes = Collections.synchronizedMap(object : LinkedHashMap<String, Long>(30, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > 50
        }
    })

    private val queueScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Start background worker to process queue sequentially
        queueScope.launch {
            for (message in messageQueue) {
                val backoffSeconds = sendHttpRequest(message)
                if (backoffSeconds > 0) {
                    // Respect Telegram 429 Retry-After response
                    delay(backoffSeconds * 1000L)
                } else {
                    // Standard pacing delay between messages
                    delay(MIN_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Enqueues a notification with instant deduplication check.
     */
    fun enqueueNotification(appName: String, title: String, text: String, formattedMessage: String) {
        val dedupKey = "$appName|$title|$text"
        val now = System.currentTimeMillis()

        synchronized(recentHashes) {
            val lastSent = recentHashes[dedupKey]
            if (lastSent != null && (now - lastSent) < DEDUP_WINDOW_MS) {
                // Duplicate detected within window; drop to protect bot from spam
                return
            }
            recentHashes[dedupKey] = now
        }

        // Try to offer to the channel; if channel is full, drops silently to prevent memory leak
        messageQueue.trySend(formattedMessage)
    }

    /**
     * Sends HTTP POST request to Telegram API.
     * Returns backoff seconds if 429 Too Many Requests was returned, or 0 on success/other errors.
     */
    private fun sendHttpRequest(text: String): Long {
        var connection: HttpURLConnection? = null
        try {
            val urlString = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 8000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

            val payload = JSONObject().apply {
                put("chat_id", CHAT_ID)
                put("text", text)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == 429) {
                // Rate limit hit: parse retry_after from response body or header
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                if (!errorStream.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(errorStream)
                        val parameters = json.optJSONObject("parameters")
                        val retryAfter = parameters?.optLong("retry_after", 5L) ?: 5L
                        return retryAfter.coerceIn(2L, 60L)
                    } catch (_: Exception) {}
                }
                return 5L
            }

            return 0L
        } catch (e: Exception) {
            e.printStackTrace()
            return 0L
        } finally {
            connection?.disconnect()
        }
    }
}
