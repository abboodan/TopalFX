package com.topal.fx.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Background listener service that intercepts incoming notifications,
 * logs them into a native SQLite database, and relays them to Telegram.
 * Also listens for two-way commands from Telegram to remotely control settings.
 */
class NotificationForwarderService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private lateinit var dbHelper: NotificationDbHelper
    private lateinit var configManager: TelegramConfigManager

    override fun onCreate() {
        super.onCreate()
        dbHelper = NotificationDbHelper(applicationContext)
        configManager = TelegramConfigManager(applicationContext)

        // Start two-way command polling loop
        startCommandPolling()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Long-polling loop to listen for commands sent from the user's verified Telegram chat.
     */
    private fun startCommandPolling() {
        serviceScope.launch {
            var lastOffset = configManager.getLastOffset()
            while (isActive) {
                try {
                    val updates = TelegramConfig.fetchUpdates(offset = lastOffset, timeoutSeconds = 20)
                    for (update in updates) {
                        // Strict security: Only process commands from the configured user
                        if (update.chatId == TelegramConfig.CHAT_ID) {
                            handleTelegramCommand(update)
                        }
                        lastOffset = update.updateId + 1
                        configManager.setLastOffset(lastOffset)
                    }
                    if (updates.isEmpty()) {
                        delay(1000)
                    }
                } catch (e: Exception) {
                    delay(8000)
                }
            }
        }
    }

    /**
     * Handles incoming interactive commands from Telegram.
     */
    private fun handleTelegramCommand(update: TelegramIncomingMessage) {
        val raw = update.text.trim()
        val parts = raw.split("\\s+".toRegex())
        val command = parts.getOrNull(0)?.lowercase() ?: ""
        val argument = if (parts.size > 1) raw.substringAfter(parts[0]).trim() else ""

        // Check if command arrived with significant delay (e.g. over 2 hours during long blackout)
        val nowSec = System.currentTimeMillis() / 1000L
        val isDelayed = (nowSec - update.date) > 7200

        when (command) {
            "/start", "/help" -> {
                sendHelpMenu(isDelayed)
            }
            "/status" -> {
                sendStatusReport(isDelayed)
            }
            "/pause", "/stop" -> {
                configManager.setPaused(true)
                val note = if (isDelayed) "\n*(⚠️ تم التنفيذ متأخراً بعد استعادة الاتصال)*" else ""
                TelegramConfig.sendReply("⏸️ تم إيقاف توجيه الإشعارات مؤقتاً.$note")
            }
            "/resume" -> {
                configManager.setPaused(false)
                val note = if (isDelayed) "\n*(⚠️ تم التنفيذ متأخراً بعد استعادة الاتصال)*" else ""
                TelegramConfig.sendReply("▶️ تم استئناف توجيه الإشعارات بنجاح.$note")
            }
            "/ignore" -> {
                if (argument.isEmpty()) {
                    TelegramConfig.sendReply("⚠️ يرجى كتابة اسم التطبيق بعد الأمر.\nمثال: `/ignore WhatsApp`")
                } else {
                    configManager.addIgnoredApp(argument)
                    TelegramConfig.sendReply("🚫 تمت إضافة [$argument] إلى قائمة الاستثناءات بنجاح.")
                }
            }
            "/unignore" -> {
                if (argument.isEmpty()) {
                    TelegramConfig.sendReply("⚠️ يرجى كتابة اسم التطبيق بعد الأمر.\nمثال: `/unignore WhatsApp`")
                } else {
                    val removed = configManager.removeIgnoredApp(argument)
                    if (removed) {
                        TelegramConfig.sendReply("✅ تمت إزالة [$argument] من قائمة الاستثناءات.")
                    } else {
                        TelegramConfig.sendReply("ℹ️ التطبيق [$argument] غير موجود في قائمة الاستثناءات أصلاً.")
                    }
                }
            }
            "/list_ignored", "/ignored" -> {
                val ignored = configManager.getIgnoredApps()
                if (ignored.isEmpty()) {
                    TelegramConfig.sendReply("📋 قائمة الاستثناءات فارغة حالياً (يتم إرسال كافة التطبيقات).")
                } else {
                    val listStr = ignored.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")
                    TelegramConfig.sendReply("📋 التطبيقات المستثناة حالياً:\n━━━━━━━━━━━━━━━\n$listStr")
                }
            }
            "/history", "/last" -> {
                val count = argument.toIntOrNull() ?: 5
                sendRecentHistory(count.coerceIn(1, 15))
            }
            else -> {
                if (command.startsWith("/")) {
                    TelegramConfig.sendReply("❓ أمر غير معروف. اكتب /help لعرض قائمة الأوامر المتاحة.")
                }
            }
        }
    }

    private fun sendHelpMenu(isDelayed: Boolean) {
        val note = if (isDelayed) "*(⚠️ متأخر بعد عودة الاتصال)*\n" else ""
        val menu = """
            $note🤖 قائمة أوامر التحكم عن بُعد - TopalFX:
            ━━━━━━━━━━━━━━━
            📊 /status - فحص حالة الهاتف والبطارية والاتصال
            ⏸️ /pause - إيقاف التوجيه مؤقتاً
            ▶️ /resume - استئناف التوجيه
            🚫 /ignore [الاسم] - استثناء تطبيق معين
            ✅ /unignore [الاسم] - إلغاء استثناء تطبيق
            📋 /list_ignored - عرض التطبيقات المستثناة
            📜 /history [العدد] - عرض آخر الإشعارات المسجلة
            ❓ /help - عرض هذه القائمة
        """.trimIndent()
        TelegramConfig.sendReply(menu)
    }

    private fun sendStatusReport(isDelayed: Boolean) {
        // Battery status
        var batteryPct = -1
        var isCharging = false
        try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                batteryPct = ((level / scale.toFloat()) * 100).toInt()
            }
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {}

        // Network status
        var netType = "غير معروف"
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(network)
            if (caps != null) {
                netType = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi 📶"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "بيانات الهاتف 📱"
                    else -> "متصل"
                }
            } else {
                netType = "غير متصل ❌"
            }
        } catch (_: Exception) {}

        val isPaused = configManager.isPaused()
        val todayCount = dbHelper.getTodayNotificationCount()
        val ignoredCount = configManager.getIgnoredApps().size
        val note = if (isDelayed) "\n*(⚠️ تقرير تم استدعاؤه بعد استعادة الاتصال)*" else ""

        val report = """
            📊 تقرير حالة الهاتف الثانوي:$note
            ━━━━━━━━━━━━━━━
            🟢 حالة التوجيه: ${if (isPaused) "موقوف مؤقتاً ⏸️" else "نشط ومستعد ▶️"}
            🔋 البطارية: $batteryPct% ${if (isCharging) "⚡ (متصل بالشاحن)" else ""}
            🌐 نوع الاتصال: $netType
            📥 إشعارات اليوم: $todayCount إشعار
            🚫 التطبيقات المستثناة: $ignoredCount تطبيق
            ━━━━━━━━━━━━━━━
            ⏰ الوقت: ${timeFormat.format(Date())}
        """.trimIndent()

        TelegramConfig.sendReply(report)
    }

    private fun sendRecentHistory(count: Int) {
        val records = dbHelper.getRecentNotifications(count)
        if (records.isEmpty()) {
            TelegramConfig.sendReply("📭 لا توجد إشعارات مسجلة في قاعدة البيانات حتى الآن.")
            return
        }

        val historyText = buildString {
            appendLine("📜 آخر $count إشعارات تم تسجيلها:")
            appendLine("━━━━━━━━━━━━━━━")
            for ((idx, r) in records.withIndex()) {
                appendLine("${idx + 1}. [${r.appName}]")
                if (r.title.isNotEmpty()) appendLine("📌 ${r.title}")
                if (r.content.isNotEmpty()) appendLine("💬 ${r.content}")
                appendLine("⏰ ${r.dateStr}")
                if (idx < records.size - 1) appendLine("───────────────")
            }
        }

        TelegramConfig.sendReply(historyText)
    }

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

        if (title.isEmpty() && text.isEmpty()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkgName
        }

        val nowMs = System.currentTimeMillis()
        val currentTime = timeFormat.format(Date(nowMs))

        // Check dynamic ignored apps
        val ignoredApps = configManager.getIgnoredApps()
        val isIgnored = ignoredApps.any { 
            pkgName.lowercase().contains(it) || appName.lowercase().contains(it) 
        }

        val isPaused = configManager.isPaused()
        val shouldForward = !isIgnored && !isPaused

        // Log everything to native database regardless of forwarding
        serviceScope.launch {
            dbHelper.insertNotification(
                packageName = pkgName,
                appName = appName,
                title = title,
                content = text,
                timestamp = nowMs,
                dateStr = currentTime,
                isForwarded = shouldForward
            )
        }

        // If forwarding is active and not ignored, dispatch to Telegram
        if (shouldForward) {
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
}
