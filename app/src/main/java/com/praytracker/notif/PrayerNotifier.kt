package com.praytracker.notif

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.praytracker.R
import com.praytracker.prayer.Prayer
import com.praytracker.ui.MainActivity

class PrayerNotifier(private val context: Context) {

    private val arabicNames = mapOf(
        Prayer.FAJR to "الفجر",
        Prayer.DHUHR to "الظهر",
        Prayer.ASR to "العصر",
        Prayer.MAGHRIB to "المغرب",
        Prayer.ISHA to "العشاء",
    )

    fun showPrayerTime(prayer: Prayer, timeText: String, soundEnabled: Boolean, vibrateEnabled: Boolean) {
        if (!hasPermission()) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannels.PRAYER_CHANNEL_ID else null
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(arabicNames[prayer] ?: prayer.displayName)
            .setContentText(context.getString(R.string.notif_prayer_time, prayer.displayName, timeText))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)

        if (!soundEnabled) builder.setSilent(true)
        if (vibrateEnabled) builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)

        manager.notify(prayer.ordinal, builder.build())
    }

    fun showReminder(prayer: Prayer, minutesBefore: Int, timeText: String) {
        if (!hasPermission()) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationChannels.REMINDER_CHANNEL_ID else null
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(context.getString(R.string.notif_reminder_title, prayer.displayName))
            .setContentText(context.getString(R.string.notif_reminder_body, minutesBefore, timeText))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        manager.notify(100 + prayer.ordinal, builder.build())
    }

    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun openApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}