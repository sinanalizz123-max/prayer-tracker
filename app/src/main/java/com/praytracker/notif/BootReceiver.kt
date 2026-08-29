package com.praytracker.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.praytracker.PrayerTrackerApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PrayerTrackerApp
        val result = goAsync()
        app.container.scope.launchSafely {
            try {
                app.container.prayerAlarmScheduler.rescheduleAll()
            } catch (_: Exception) {
            } finally {
                result.finish()
            }
        }
    }
}

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PrayerTrackerApp
        val result = goAsync()
        app.container.scope.launchSafely {
            try {
                app.container.prayerAlarmScheduler.rescheduleAll()
            } catch (_: Exception) {
            } finally {
                result.finish()
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchSafely(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    kotlinx.coroutines.launch(block = block)
}