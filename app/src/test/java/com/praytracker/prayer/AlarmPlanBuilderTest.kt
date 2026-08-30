package com.praytracker.prayer

import com.praytracker.data.PrayerSettings
import com.praytracker.util.AlarmPlanBuilder
import com.praytracker.util.PrayerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmPlanBuilderTest {

    private class TestSettings : PrayerSettings {
        override var calculationMethod: Int = 3
        override var madhab: Int = 0
        override var highLatitudeRule: Int = 0
        override var adjustmentFajr: Int = 0
        override var adjustmentDhuhr: Int = 0
        override var adjustmentAsr: Int = 0
        override var adjustmentMaghrib: Int = 0
        override var adjustmentIsha: Int = 0
        override var hijriAdjustment: Int = 0
        override var locationName: String = "Makkah"
    }

    private val makkahLat = 21.4225
    private val makkahLon = 39.8262
    private val riyadh = "Asia/Riyadh"
    private val date = LocalDate.of(2026, 8, 15)

    private val scheduleToday = PrayerCalculator.calculateSchedule(
        makkahLat, makkahLon, riyadh, date, TestSettings()
    )
    private val scheduleTomorrow = PrayerCalculator.calculateSchedule(
        makkahLat, makkahLon, riyadh, date.plusDays(1), TestSettings()
    )

    private fun flags(
        master: Boolean = true,
        fajr: Boolean = true,
        dhuhr: Boolean = true,
        asr: Boolean = true,
        maghrib: Boolean = true,
        isha: Boolean = true,
        reminderDelay: Int = 15
    ) = AlarmPlanBuilder.NotificationFlags(
        masterEnabled = master,
        fajrEnabled = fajr,
        dhuhrEnabled = dhuhr,
        asrEnabled = asr,
        maghribEnabled = maghrib,
        ishaEnabled = isha,
        reminderDelayMinutes = reminderDelay
    )

    private val beforeFajr = ZonedDateTime.of(2026, 8, 15, 1, 0, 0, 0, ZoneId.of(riyadh))

    @Test
    fun `master disabled yields empty plan`() {
        val plan = AlarmPlanBuilder.buildPlan(scheduleToday, scheduleTomorrow, beforeFajr, flags(master = false))
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `all enabled with follow-ups yields twenty requests`() {
        val plan = AlarmPlanBuilder.buildPlan(scheduleToday, scheduleTomorrow, beforeFajr, flags(reminderDelay = 15))
        assertEquals(20, plan.size)
    }

    @Test
    fun `no follow-up delay yields on-time requests only`() {
        val plan = AlarmPlanBuilder.buildPlan(scheduleToday, scheduleTomorrow, beforeFajr, flags(reminderDelay = 0))
        assertEquals(10, plan.size)
        assertTrue(plan.none { it.isFollowUp })
    }

    @Test
    fun `request codes match the cancel ranges`() {
        val plan = AlarmPlanBuilder.buildPlan(scheduleToday, scheduleTomorrow, beforeFajr, flags(reminderDelay = 15))
        val codes = plan.map { it.requestCode }.toSet()
        val expected = (0..4).union(10..14).union(20..24).union(30..34).toSet()
        assertEquals(expected, codes)
    }

    @Test
    fun `only fajr enabled schedules fajr for both days`() {
        val plan = AlarmPlanBuilder.buildPlan(
            scheduleToday, scheduleTomorrow, beforeFajr,
            flags(fajr = true, dhuhr = false, asr = false, maghrib = false, isha = false, reminderDelay = 0)
        )

        assertEquals(2, plan.size)
        assertTrue(plan.all { it.prayerName == "FAJR" })
        assertEquals(listOf(0, 10), plan.map { it.requestCode }.sorted())

        val expectedEpochs = listOf(
            scheduleToday.fajr.toInstant().toEpochMilli(),
            scheduleTomorrow.fajr.toInstant().toEpochMilli()
        ).sorted()
        assertEquals(expectedEpochs, plan.map { it.triggerEpochMillis }.sorted())
    }

    @Test
    fun `past prayers today are omitted but tomorrow remains`() {
        val now = scheduleToday.dhuhr.plusMinutes(30)
        val plan = AlarmPlanBuilder.buildPlan(scheduleToday, scheduleTomorrow, now, flags(reminderDelay = 0))

        val todayFajr = plan.firstOrNull { it.prayerName == "FAJR" && it.requestCode < 10 }
        assertTrue("today's Fajr is in the past and must be skipped", todayFajr == null)

        assertTrue(plan.any { it.prayerName == "ASR" && it.requestCode in 0..4 })
        assertTrue(plan.any { it.prayerName == "MAGHRIB" && it.requestCode in 0..4 })

        assertTrue("tomorrow's Fajr must remain", plan.any { it.prayerName == "FAJR" && it.requestCode == 10 })
    }

    @Test
    fun `follow-up request lands after the prayer time`() {
        val plan = AlarmPlanBuilder.buildPlan(scheduleToday, scheduleTomorrow, beforeFajr, flags(reminderDelay = 5))
        val maghrib = plan.first { it.prayerName == "MAGHRIB" && !it.isFollowUp && it.requestCode < 10 }
        val followUp = plan.first { it.prayerName == "MAGHRIB" && it.isFollowUp && it.requestCode < 30 }

        assertEquals(maghrib.triggerEpochMillis + 5 * 60_000L, followUp.triggerEpochMillis)
        assertEquals(23, followUp.requestCode)
        assertFalse(plan.any { it.triggerEpochMillis < beforeFajr.toInstant().toEpochMilli() })
    }
}