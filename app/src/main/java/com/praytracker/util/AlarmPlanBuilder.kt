package com.praytracker.util

import java.time.ZonedDateTime

data class AlarmPlanItem(
    val prayerName: String,
    val triggerEpochMillis: Long,
    val requestCode: Int,
    val isFollowUp: Boolean
)

/**
 * Decides which alarm requests should be armed for the two-day scheduling
 * horizon. Pure JVM logic (no Android types) so it can be unit tested.
 *
 * Request codes intentionally mirror the fixed code ranges used by
 * [com.praytracker.util.AlarmScheduler] when cancelling alarms:
 * today on-time 0-4, tomorrow on-time 10-14, today follow-up 20-24,
 * tomorrow follow-up 30-34.
 */
object AlarmPlanBuilder {

    data class NotificationFlags(
        val masterEnabled: Boolean,
        val fajrEnabled: Boolean,
        val dhuhrEnabled: Boolean,
        val asrEnabled: Boolean,
        val maghribEnabled: Boolean,
        val ishaEnabled: Boolean,
        val reminderDelayMinutes: Int
    )

    private const val TOMORROW_OFFSET = 10
    private const val FOLLOW_UP_OFFSET = 20

    fun buildPlan(
        todaySchedule: PrayerCalculator.PrayerSchedule,
        tomorrowSchedule: PrayerCalculator.PrayerSchedule,
        now: ZonedDateTime,
        flags: NotificationFlags
    ): List<AlarmPlanItem> {
        if (!flags.masterEnabled) return emptyList()

        val plan = mutableListOf<AlarmPlanItem>()
        appendDay(plan, todaySchedule, now, offsetCode = 0, flags)
        appendDay(plan, tomorrowSchedule, now, offsetCode = TOMORROW_OFFSET, flags)
        return plan
    }

    private fun appendDay(
        plan: MutableList<AlarmPlanItem>,
        schedule: PrayerCalculator.PrayerSchedule,
        now: ZonedDateTime,
        offsetCode: Int,
        flags: NotificationFlags
    ) {
        val prayers = listOf(
            Triple("FAJR", schedule.fajr, flags.fajrEnabled),
            Triple("DHUHR", schedule.dhuhr, flags.dhuhrEnabled),
            Triple("ASR", schedule.asr, flags.asrEnabled),
            Triple("MAGHRIB", schedule.maghrib, flags.maghribEnabled),
            Triple("ISHA", schedule.isha, flags.ishaEnabled)
        )

        for ((index, prayer) in prayers.withIndex()) {
            val (name, time, enabled) = prayer
            if (!enabled || !time.isAfter(now)) continue

            plan.add(
                AlarmPlanItem(
                    prayerName = name,
                    triggerEpochMillis = time.toInstant().toEpochMilli(),
                    requestCode = offsetCode + index,
                    isFollowUp = false
                )
            )

            val reminderDelay = flags.reminderDelayMinutes
            if (reminderDelay > 0) {
                plan.add(
                    AlarmPlanItem(
                        prayerName = name,
                        triggerEpochMillis = time.plusMinutes(reminderDelay.toLong()).toInstant().toEpochMilli(),
                        requestCode = offsetCode + FOLLOW_UP_OFFSET + index,
                        isFollowUp = true
                    )
                )
            }
        }
    }
}