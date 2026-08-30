package com.praytracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.praytracker.data.SettingsManager
import java.time.ZonedDateTime

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /** Snapshot of currently armed alarms (request code -> trigger epoch millis). */
    private const val PLAN_PREFS = "alarm_plan_prefs"

    fun rescheduleAlarms(context: Context, settings: SettingsManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!settings.isMasterNotificationEnabled) {
            // Reminders are switched off globally: drop everything and forget the arming plan.
            cancelAllAlarms(context)
            writeArmedPlan(context, emptyMap())
            Log.d(TAG, "Master notifications disabled. Cancelled all alarms.")
            return
        }

        val now = ZonedDateTime.now()
        val day = now.toLocalDate()

        // Schedules are recomputed here so alarms always reflect the persisted
        // settings, independently of any UI state or cached ViewModel schedules.
        val todaySchedule = PrayerCalculator.calculateSchedule(
            lat = settings.latitude,
            lon = settings.longitude,
            timezoneId = settings.timezoneId,
            localDate = day,
            settings = settings
        )
        val tomorrowSchedule = PrayerCalculator.calculateSchedule(
            lat = settings.latitude,
            lon = settings.longitude,
            timezoneId = settings.timezoneId,
            localDate = day.plusDays(1),
            settings = settings
        )

        val plan = AlarmPlanBuilder.buildPlan(
            todaySchedule = todaySchedule,
            tomorrowSchedule = tomorrowSchedule,
            now = now,
            flags = AlarmPlanBuilder.NotificationFlags(
                masterEnabled = settings.isMasterNotificationEnabled,
                fajrEnabled = settings.isFajrNotificationEnabled,
                dhuhrEnabled = settings.isDhuhrNotificationEnabled,
                asrEnabled = settings.isAsrNotificationEnabled,
                maghribEnabled = settings.isMaghribNotificationEnabled,
                ishaEnabled = settings.isIshaNotificationEnabled,
                reminderDelayMinutes = settings.reminderDelayMinutes
            )
        )

        val desired = plan.associate { it.requestCode to it.triggerEpochMillis }
        val armed = readArmedPlan(context)

        // Only touch what changed: cancel alarms that are no longer wanted, and
        // (re)arm those whose trigger time moved. Armed-and-unchanged alarms stay
        // exactly as AlarmManager registered them, so a notification firing no
        // longer tears down and rebuilds the whole schedule.
        for (code in armed.keys) {
            if (code !in desired) {
                cancelAlarmIfPresent(context, code)
            }
        }
        for (item in plan) {
            if (armed[item.requestCode] != item.triggerEpochMillis) {
                scheduleAlarm(context, alarmManager, item.triggerEpochMillis, item.prayerName, item.requestCode, item.isFollowUp)
            }
        }

        writeArmedPlan(context, desired)
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
        for (code in listOf(0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 20, 21, 22, 23, 24, 30, 31, 32, 33, 34)) {
            cancelAlarmIfPresent(context, code)
        }
    }

    private fun cancelAlarmIfPresent(context: Context, code: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.praytracker.action.PRAYER_ALARM"
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

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

    private fun readArmedPlan(context: Context): Map<Int, Long> {
        val prefs = context.getSharedPreferences(PLAN_PREFS, Context.MODE_PRIVATE)
        return listOf(0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 20, 21, 22, 23, 24, 30, 31, 32, 33, 34)
            .mapNotNull { code ->
                val trigger = prefs.getLong("armed_$code", -1L)
                if (trigger >= 0L) code to trigger else null
            }
            .toMap()
    }

    private fun writeArmedPlan(context: Context, plan: Map<Int, Long>) {
        val prefs = context.getSharedPreferences(PLAN_PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (code in listOf(0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 20, 21, 22, 23, 24, 30, 31, 32, 33, 34)) {
            val trigger = plan[code]
            if (trigger != null) {
                editor.putLong("armed_$code", trigger)
            } else {
                editor.remove("armed_$code")
            }
        }
        editor.apply()
    }
}
