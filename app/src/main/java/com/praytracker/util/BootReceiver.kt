package com.praytracker.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.praytracker.data.SettingsManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Device reboot completed. Rescheduling prayer alarms...")
            val settings = SettingsManager(context)
            AlarmScheduler.rescheduleAlarms(context, settings)
        }
    }
}
