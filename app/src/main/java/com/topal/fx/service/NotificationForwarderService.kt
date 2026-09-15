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

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return

        // 1. Ignore persistent/ongoing notifications (e.g. charging status, USB connected, active downloads)
        if (sbn.isOngoing) return

        // 2. Ignore notifications posted by our own app
        if (pkgName == packageName) return

        // 3. Ignore Telegram notifications to avoid recursive forwarding loops
        if (pkgName.contains("telegram", ignoreCase = true)) return

        // 4. Ignore Android core system and Google Play services noise
        if (pkgName == "android" || pkgName == "com.android.systemui" || pkgName == "com.google.android.gms") return

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

        TelegramConfig.enqueueNotification(appName, title, text, formattedMessage)
    }
}
