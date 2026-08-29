package com.praytracker.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {

    const val PRAYER_CHANNEL_ID = "prayer_time"
    const val REMINDER_CHANNEL_ID = "prayer_reminder"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prayer = NotificationChannel(
            PRAYER_CHANNEL_ID,
            "Prayer time alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Fired when a prayer time arrives"
            enableVibration(true)
        }
        val reminder = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Prayer reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders shortly before a prayer time"
            enableVibration(true)
        }
        manager.createNotificationChannel(prayer)
        manager.createNotificationChannel(reminder)
    }
}