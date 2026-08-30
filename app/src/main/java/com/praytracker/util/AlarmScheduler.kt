package com.praytracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.praytracker.data.SettingsManager
import java.time.LocalDate
import java.time.ZonedDateTime

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    fun rescheduleAlarms(context: Context, settings: SettingsManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // First, cancel any existing alarms to prevent duplication
        cancelAllAlarms(context)

        if (!settings.isMasterNotificationEnabled) {
            Log.d(TAG, "Master notifications disabled. Cancelled all alarms.")
            return
        }

        val now = ZonedDateTime.now()
        val today = LocalDate.now()

        // Schedule for today and tomorrow
        scheduleForDate(context, alarmManager, today, now, settings, offsetCode = 0)
        scheduleForDate(context, alarmManager, today.plusDays(1), now, settings, offsetCode = 10)
    }

    private fun scheduleForDate(
        context: Context,
        alarmManager: AlarmManager,
        date: LocalDate,
        now: ZonedDateTime,
        settings: SettingsManager,
        offsetCode: Int
    ) {
        val schedule = PrayerCalculator.calculateSchedule(
            lat = settings.latitude,
            lon = settings.longitude,
            timezoneId = settings.timezoneId,
            localDate = date,
            settings = settings
        )

        val prayersToSchedule = listOf(
            Triple("FAJR", schedule.fajr, settings.isFajrNotificationEnabled),
            Triple("DHUHR", schedule.dhuhr, settings.isDhuhrNotificationEnabled),
            Triple("ASR", schedule.asr, settings.isAsrNotificationEnabled),
            Triple("MAGHRIB", schedule.maghrib, settings.isMaghribNotificationEnabled),
            Triple("ISHA", schedule.isha, settings.isIshaNotificationEnabled)
        )

        for ((index, item) in prayersToSchedule.withIndex()) {
            val (name, time, isEnabled) = item
            if (isEnabled && time.isAfter(now)) {
                val requestCode = offsetCode + index
                scheduleAlarm(context, alarmManager, time.toInstant().toEpochMilli(), name, requestCode, isFollowUp = false)

                // Follow-up reminder a few minutes after the prayer time begins
                val delayMinutes = settings.reminderDelayMinutes
                if (delayMinutes > 0) {
                    scheduleAlarm(
                        context,
                        alarmManager,
                        time.plusMinutes(delayMinutes.toLong()).toInstant().toEpochMilli(),
                        name,
                        requestCode = (offsetCode + 20) + index,
                        isFollowUp = true
                    )
                }
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerTimeMs: Long,
        prayerName: String,
        requestCode: Int,
        isFollowUp: Boolean
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.praytracker.action.PRAYER_ALARM"
            putExtra("PRAYER_NAME", prayerName)
            putExtra("IS_FOLLOW_UP", isFollowUp)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for $prayerName at $triggerTimeMs with request code $requestCode")
        } catch (e: SecurityException) {
            // In case exact alarm permission is denied on Android 12+
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled fallback inexact alarm for $prayerName due to security exception")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to schedule alarm for $prayerName", ex)
            }
        }
    }

    private fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.praytracker.action.PRAYER_ALARM"
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        // Cancel codes for today (0-4), tomorrow (10-14),
        // and follow-up reminders today (20-24) and tomorrow (30-34)
        for (code in listOf(0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 20, 21, 22, 23, 24, 30, 31, 32, 33, 34)) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                flags
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
