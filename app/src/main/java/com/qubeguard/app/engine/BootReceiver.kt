package com.qubeguard.app.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Automatically starts QubeGuard protection service upon device boot completed.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val prefs = context.getSharedPreferences("qubeguard_settings", Context.MODE_PRIVATE)
            val autoStartOnBoot = prefs.getBoolean("autostart_on_boot", true)
            if (autoStartOnBoot) {
                val serviceIntent = Intent(context, QubeGuardService::class.java).apply {
                    action = QubeGuardService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
