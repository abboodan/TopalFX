package com.topal.fx.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ensures TopalFX Pro background components are recognized and re-awakened
 * automatically when the device boots up or after app updates.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Waking the process allows Android's NotificationManagerService
            // to automatically bind NotificationForwarderService if permission was granted.
        }
    }
}
