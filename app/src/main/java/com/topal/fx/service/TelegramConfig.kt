package com.topal.fx.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Built-in configuration and dispatcher for Telegram Bot notification forwarding.
 */
object TelegramConfig {
    const val BOT_TOKEN: String = "8921837204:AAF0QB1IIYMQmoW6lXLtfU6U0hH-lmk2GRA"
    const val CHAT_ID: String = "969346296"

    /**
     * Sends a message to the configured Telegram chat via HTTP POST.
     * Non-blocking, light-weight, and resilient to network failures.
     */
    suspend fun sendMessage(text: String): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlString = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
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
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            connection?.disconnect()
        }
    }
}
