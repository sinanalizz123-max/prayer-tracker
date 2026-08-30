package com.praytracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.praytracker.MainActivity
import com.praytracker.data.SettingsManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        Log.d("AlarmReceiver", "Received prayer alarm for: $prayerName")

        val settings = SettingsManager(context)
        
        // Show notification
        showNotification(context, prayerName, settings)

        // Self-healing: reschedule alarms for the next 24 hours
        AlarmScheduler.rescheduleAlarms(context, settings)
    }

    private fun showNotification(context: Context, prayerName: String, settings: SettingsManager) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_times_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Prayer Alerts"
            val descriptionText = "Notifications for Islamic daily prayer times"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action intent to open the app on click
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, flags)

        // Custom notification title and body
        val title = "Time for $prayerName"
        val text = "The time for $prayerName has started. Tap to view the prayer schedule."

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // simple system alarm icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Use custom sound settings if enabled (otherwise defaults to system notification sound)
        if (!settings.isCustomSoundEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        }

        // Notification IDs: 1 for Fajr, 2 for Dhuhr, etc.
        val notificationId = when (prayerName.uppercase()) {
            "FAJR" -> 1
            "DHUHR" -> 2
            "ASR" -> 3
            "MAGHRIB" -> 4
            "ISHA" -> 5
            else -> 100
        }

        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d("AlarmReceiver", "Notification posted for $prayerName")
        } catch (e: SecurityException) {
            Log.e("AlarmReceiver", "Failed to post notification due to permission check", e)
        }
    }
}
