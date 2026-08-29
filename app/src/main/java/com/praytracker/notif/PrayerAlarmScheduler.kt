package com.praytracker.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.praytracker.data.repo.PrayerTimesRepository
import com.praytracker.data.settings.SettingsRepository
import com.praytracker.prayer.Prayer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Schedules exact prayer-time alarms and handles the rolling chain so that
 * alarms keep firing even if the app is not reopened. Times are computed by
 * [PrayerTimesRepository] from persisted settings, so rescheduling picks up any
 * changes instantly.
 */
class PrayerAlarmScheduler(
    private val context: Context,
    private val timesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun rescheduleAll() {
        val settings = settingsRepository.snapshot()
        NotificationChannels.create(context)
        if (!settings.notificationsEnabled) {
            cancelAll()
            return
        }
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        for (daysAhead in 0..1) {
            val day = today.plusDays(daysAhead.toLong())
            val times = timesRepository.timesFor(day)
            for (prayer in Prayer.ORDER) {
                if (!settings.notifEnabledFor(prayer)) {
                    cancel(prayer, day)
                    continue
                }
                val at = times.localDateTime(prayer) ?: continue
                if (at.isAfter(now)) {
                    if (Build.VERSION.SDK_INT.hasSchedulePermission(alarmManager)) {
                        schedulePrayer(prayer, day, at, settings.reminderEnabled, settings.reminderMinutes)
                    } else {
                        scheduleInexact(prayer, day, at)
                    }
                }
            }
        }
    }

    private fun scheduleInexact(prayer: Prayer, day: LocalDate, at: LocalDateTime) {
        val intent = alarmIntent(prayer, day, at, isReminder = false)
        val pending = toPendingIntent(intent, requestCode(prayer, day, isReminder = false))
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMillis(), pending)
        }
    }

    private suspend fun schedulePrayer(prayer: Prayer, day: LocalDate, at: LocalDateTime, reminderEnabled: Boolean, reminderMinutes: Int) {
        val now = LocalDateTime.now()
        if (at.isAfter(now)) setExact(alarmIntent(prayer, day, at, isReminder = false), requestCode(prayer, day, isReminder = false), at)
        if (reminderEnabled && at.minusMinutes(reminderMinutes.toLong()).isAfter(now)) {
            val remAt = at.minusMinutes(reminderMinutes.toLong())
            setExact(alarmIntent(prayer, day, remAt, isReminder = true), requestCode(prayer, day, isReminder = true), remAt)
        }
    }

    /** Called from the alarm receiver: schedule the same prayer's next occurrence. */
    suspend fun scheduleNextOccurrence(prayer: Prayer, firedDate: LocalDate, wasReminder: Boolean) {
        val settings = settingsRepository.snapshot()
        if (!settings.notificationsEnabled || !settings.notifEnabledFor(prayer)) return
        if (!Build.VERSION.SDK_INT.hasSchedulePermission(alarmManager)) return
        val nextDay = firedDate.plusDays(1)
        val times = timesRepository.timesFor(nextDay)
        val at = times.localDateTime(prayer) ?: return
        if (wasReminder && settings.reminderEnabled) {
            val remAt = at.minusMinutes(settings.reminderMinutes.toLong())
            if (remAt.isAfter(LocalDateTime.now())) {
                setExact(alarmIntent(prayer, nextDay, remAt, isReminder = true), requestCode(prayer, nextDay, isReminder = true), remAt)
            }
        } else {
            setExact(alarmIntent(prayer, nextDay, at, isReminder = false), requestCode(prayer, nextDay, isReminder = false), at)
            if (settings.reminderEnabled) {
                val remAt = at.minusMinutes(settings.reminderMinutes.toLong())
                if (remAt.isAfter(LocalDateTime.now())) {
                    setExact(alarmIntent(prayer, nextDay, remAt, isReminder = true), requestCode(prayer, nextDay, isReminder = true), remAt)
                }
            }
        }
    }

    fun cancel(prayer: Prayer, day: LocalDate) {
        cancelPending(requestCode(prayer, day, isReminder = false))
        cancelPending(requestCode(prayer, day, isReminder = true))
    }

    fun cancelAll() {
        // Since request codes depend on day-of-year, canceling yesterday/today/tomorrow suffices.
        val today = LocalDate.now()
        for (p in Prayer.ORDER) {
            cancel(p, today.minusDays(1))
            cancel(p, today)
            cancel(p, today.plusDays(1))
        }
    }

    private fun setExact(intent: Intent, requestCode: Int, at: LocalDateTime) {
        val pending = toPendingIntent(intent, requestCode)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMillis(), pending)
        } catch (_: SecurityException) {
            runCatching { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMillis(), pending) }
        }
    }

    private fun cancelPending(requestCode: Int) {
        runCatching {
            val pending = toPendingIntent(Intent(context, PrayerAlarmReceiver::class.java), requestCode)
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun toPendingIntent(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun alarmIntent(prayer: Prayer, day: LocalDate, at: LocalDateTime, isReminder: Boolean): Intent =
        Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = if (isReminder) ACTION_REMINDER else ACTION_PRAYER
            putExtra(EXTRA_PRAYER, prayer.name)
            putExtra(EXTRA_DATE, day.toString())
            putExtra(EXTRA_TIME, at.format(timeFormatter))
        }

    private fun requestCode(prayer: Prayer, day: LocalDate, isReminder: Boolean): Int {
        val base = if (isReminder) 50000 else 0
        return base + prayer.ordinal * 10000 + day.dayOfYear
    }

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    companion object {
        const val ACTION_PRAYER = "com.praytracker.action.PRAYER_TIME"
        const val ACTION_REMINDER = "com.praytracker.action.PRAYER_REMINDER"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
    }
}

private fun Int.hasSchedulePermission(alarmManager: AlarmManager): Boolean =
    if (this >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }