package com.topal.fx.service

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent runtime configuration for the Telegram Notification Relay.
 * Settings persist across app updates, battery loss, and system reboots.
 */
class TelegramConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "topalfx_telegram_relay_config"
        private const val KEY_IS_PAUSED = "is_paused"
        private const val KEY_IGNORED_APPS = "ignored_apps"
        private const val KEY_LAST_OFFSET = "last_telegram_offset"
    }

    fun isPaused(): Boolean {
        return prefs.getBoolean(KEY_IS_PAUSED, false)
    }

    fun setPaused(paused: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PAUSED, paused).apply()
    }

    fun getIgnoredApps(): Set<String> {
        return prefs.getStringSet(KEY_IGNORED_APPS, emptySet()) ?: emptySet()
    }

    fun addIgnoredApp(appNameOrPkg: String): Boolean {
        val clean = appNameOrPkg.trim().lowercase()
        if (clean.isEmpty()) return false
        val current = getIgnoredApps().toMutableSet()
        val added = current.add(clean)
        if (added) {
            prefs.edit().putStringSet(KEY_IGNORED_APPS, current).apply()
        }
        return added
    }

    fun removeIgnoredApp(appNameOrPkg: String): Boolean {
        val clean = appNameOrPkg.trim().lowercase()
        val current = getIgnoredApps().toMutableSet()
        val removed = current.remove(clean)
        if (removed) {
            prefs.edit().putStringSet(KEY_IGNORED_APPS, current).apply()
        }
        return removed
    }

    fun getLastOffset(): Long {
        return prefs.getLong(KEY_LAST_OFFSET, 0L)
    }

    fun setLastOffset(offset: Long) {
        prefs.edit().putLong(KEY_LAST_OFFSET, offset).apply()
    }
}
