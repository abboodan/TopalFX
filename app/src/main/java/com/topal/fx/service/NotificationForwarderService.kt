package com.topal.fx.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Resilient 24/7 background listener service that intercepts incoming notifications,
 * logs them into a native SQLite database with pending/sent status tracking,
 * relays them to Telegram, automatically syncs queued notifications upon network reconnection,
 * and maintains continuous operation via auto-rebind.
 */
class NotificationForwarderService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private lateinit var dbHelper: NotificationDbHelper
    private lateinit var configManager: TelegramConfigManager

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val isSyncingPending = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        dbHelper = NotificationDbHelper(applicationContext)
        configManager = TelegramConfigManager(applicationContext)

        // Register dynamic network monitor for instant offline sync upon reconnection
        registerNetworkMonitor()

        // Start two-way command polling loop
        startCommandPolling()
    }

    override fun onDestroy() {
        unregisterNetworkMonitor()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Automatic recovery: If Android OS unbinds this listener due to memory pressure or OEM power saving,
     * immediately request rebind to ensure uninterrupted 24/7 background operation.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotificationForwarderService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Registers modern NetworkCallback to detect when internet becomes available
     * and trigger immediate offline queue flush.
     */
    private fun registerNetworkMonitor() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val cm = connectivityManager ?: return

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    syncPendingNotifications()
                }
            }
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterNetworkMonitor() {
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
        } catch (_: Exception) {}
        networkCallback = null
    }

    /**
     * Flushes all stored pending notifications (STATUS_PENDING) in chronological order
     * once network connectivity is verified.
     */
    private fun syncPendingNotifications() {
        if (!isSyncingPending.compareAndSet(false, true)) return

        serviceScope.launch {
            try {
                val pendingList = dbHelper.getPendingNotifications(limit = 100)
                if (pendingList.isNotEmpty()) {
                    for (record in pendingList) {
                        if (!isActive) break

                        val isDelayed = (System.currentTimeMillis() - record.timestamp) > 60_000L
                        val delayNote = if (isDelayed) "\n*(⏰ تم الإرسال بعد استعادة الاتصال)*" else ""

                        val message = buildString {
                            appendLine("🔔 إشعار جديد$delayNote")
                            appendLine("━━━━━━━━━━━━━━━")
                            appendLine("📱 التطبيق: ${record.appName}")
                            if (record.title.isNotEmpty()) {
                                appendLine("📌 العنوان: ${record.title}")
                            }
                            if (record.content.isNotEmpty()) {
                                appendLine("💬 المحتوى: ${record.content}")
                            }
                            appendLine("⏰ الوقت: ${record.dateStr}")
                            appendLine("━━━━━━━━━━━━━━━")
                            append("🚀 عبر TopalFX Pro Gateway")
                        }

                        val delivered = TelegramConfig.sendDirectSync(message)
                        if (delivered) {
                            dbHelper.markAsSent(record.id)
                            delay(1600L) // Safe anti-spam interval
                        } else {
                            // If connection drops again during sync, abort current pass
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSyncingPending.set(false)
            }
        }
    }

    /**
     * Long-polling loop to listen for commands sent from verified Telegram chat.
     * Backs off gracefully when internet is down to prevent battery drain.
     */
    private fun startCommandPolling() {
        serviceScope.launch {
            var lastOffset = configManager.getLastOffset()
            while (isActive) {
                try {
                    val updates = TelegramConfig.fetchUpdates(offset = lastOffset, timeoutSeconds = 20)
                    if (updates == null) {
                        // Offline or host unreachable: wait 15 seconds to conserve battery
                        delay(15_000L)
                        continue
                    }

                    for (update in updates) {
                        try {
                            // Strict security: Only process commands from configured user
                            if (update.chatId == TelegramConfig.CHAT_ID) {
                                handleTelegramCommand(update)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            lastOffset = update.updateId + 1
                            configManager.setLastOffset(lastOffset)
                        }
                    }

                    if (updates.isEmpty()) {
                        delay(1000L)
                    }
                } catch (e: Exception) {
                    delay(8000L)
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
                // Check if any pending notifications should now be sent
                syncPendingNotifications()
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
            "/sync" -> {
                val pendingCount = dbHelper.getPendingNotifications(limit = 100).size
                TelegramConfig.sendReply("🔄 جاري مزامنة الإشعارات المعلقة... (المتبقي: $pendingCount)")
                syncPendingNotifications()
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
            🔄 /sync - إجبار مزامنة الإشعارات المعلقة
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
        val pendingCount = dbHelper.getPendingNotifications(limit = 100).size
        val note = if (isDelayed) "\n*(⚠️ تقرير تم استدعاؤه بعد استعادة الاتصال)*" else ""

        val report = """
            📊 تقرير حالة الهاتف الثانوي:$note
            ━━━━━━━━━━━━━━━
            🟢 حالة التوجيه: ${if (isPaused) "موقوف مؤقتاً ⏸️" else "نشط ومستعد ▶️"}
            🔋 البطارية: $batteryPct% ${if (isCharging) "⚡ (متصل بالشاحن)" else ""}
            🌐 نوع الاتصال: $netType
            📥 إشعارات اليوم: $todayCount إشعار
            ⏳ إشعارات قيد المزامنة: $pendingCount معلق
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
                val statusIcon = when (r.status) {
                    NotificationDbHelper.STATUS_SENT -> "✅"
                    NotificationDbHelper.STATUS_PENDING -> "⏳"
                    NotificationDbHelper.STATUS_IGNORED -> "🚫"
                    else -> "ℹ️"
                }
                appendLine("${idx + 1}. [${r.appName}] $statusIcon")
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

        val status = when {
            isIgnored || isPaused -> NotificationDbHelper.STATUS_IGNORED
            else -> NotificationDbHelper.STATUS_PENDING
        }

        serviceScope.launch {
            val recordId = dbHelper.insertNotification(
                packageName = pkgName,
                appName = appName,
                title = title,
                content = text,
                timestamp = nowMs,
                dateStr = currentTime,
                status = status
            )

            // If eligible to forward, dispatch to Telegram queue with onSent callback
            if (status == NotificationDbHelper.STATUS_PENDING) {
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

                TelegramConfig.enqueueNotification(appName, title, text, formattedMessage) {
                    if (recordId > 0) {
                        dbHelper.markAsSent(recordId)
                    }
                }
            }
        }
    }
}
