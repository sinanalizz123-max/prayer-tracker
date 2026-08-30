package com.praytracker.prayer

import com.praytracker.data.PrayerSettings
import com.praytracker.util.PrayerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import java.time.ZonedDateTime

class PrayerCalculatorTest {

    private class TestSettings(
        override var calculationMethod: Int = 3,
        override var madhab: Int = 0,
        override var highLatitudeRule: Int = 0,
        override var adjustmentFajr: Int = 0,
        override var adjustmentDhuhr: Int = 0,
        override var adjustmentAsr: Int = 0,
        override var adjustmentMaghrib: Int = 0,
        override var adjustmentIsha: Int = 0,
        override var hijriAdjustment: Int = 0,
        override var locationName: String = "Test Location"
    ) : PrayerSettings

    private val makkahLat = 21.4225
    private val makkahLon = 39.8262
    private val riyadh = "Asia/Riyadh"
    private val date = LocalDate.of(2026, 8, 15)

    private val settings = TestSettings(locationName = "Makkah")

    private fun min(t: ZonedDateTime): ZonedDateTime = t.truncatedTo(ChronoUnit.MINUTES)

    @Test
    fun `schedule has consistent ordering and date for makkah`() {
        val schedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, settings)

        assertEquals(date, schedule.date)
        assertTrue(schedule.fajr.isBefore(schedule.sunrise))
        assertTrue(schedule.sunrise.isBefore(schedule.dhuhr))
        assertTrue(schedule.dhuhr.isBefore(schedule.asr))
        assertTrue(schedule.asr.isBefore(schedule.maghrib))
        assertTrue(schedule.maghrib.isBefore(schedule.isha))

        assertEquals(6, schedule.list.size)
        assertEquals(listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA"), schedule.list.map { it.type })
        assertEquals(listOf(false, true, false, false, false, false), schedule.list.map { it.isSunrise })

        val timePattern = Regex("""\d{1,2}:\d{2} (AM|PM)""")
        schedule.list.forEach { item ->
            assertTrue("formatted time for ${item.type} matches pattern", timePattern.matches(item.formattedTime))
            assertEquals(date, item.time.toLocalDate())
        }

        assertEquals("Makkah", schedule.locationName)
        assertTrue(schedule.hijriDateEn.startsWith("2 Rabi' al-Awwal 1448"))
    }

    @Test
    fun `next prayer is asr in the afternoon`() {
        val now = ZonedDateTime.of(2026, 8, 15, 15, 0, 0, 0, ZoneId.of(riyadh))
        val next = PrayerCalculator.getNextPrayer(makkahLat, makkahLon, riyadh, now, settings)

        assertEquals("ASR", next.name)
        assertTrue(next.countdownMinutes in 1..180)
        assertTrue(next.countdownSeconds in 0..59)
    }

    @Test
    fun `next prayer rolls over to tomorrow fajr after isha`() {
        val now = ZonedDateTime.of(2026, 8, 15, 23, 30, 0, 0, ZoneId.of(riyadh))
        val next = PrayerCalculator.getNextPrayer(makkahLat, makkahLon, riyadh, now, settings)

        assertEquals("FAJR", next.name)
        assertEquals(LocalDate.of(2026, 8, 16), next.targetTime.toLocalDate())
    }

    @Test
    fun `manual offset shifts fajr only`() {
        val base = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, settings)

        settings.adjustmentFajr = -5
        val adjusted = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, settings)

        assertEquals(min(base.fajr).minusMinutes(5), min(adjusted.fajr))
        assertEquals(min(base.dhuhr), min(adjusted.dhuhr))
        assertEquals(min(base.isha), min(adjusted.isha))
    }

    @Test
    fun `offsets apply to dhuhr asr maghrib isha too`() {
        val base = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, settings)

        settings.adjustmentDhuhr = 2
        settings.adjustmentAsr = -3
        settings.adjustmentMaghrib = 4
        settings.adjustmentIsha = -1

        val schedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, settings)

        assertEquals(min(base.dhuhr).plusMinutes(2), min(schedule.dhuhr))
        assertEquals(min(base.asr).minusMinutes(3), min(schedule.asr))
        assertEquals(min(base.maghrib).plusMinutes(4), min(schedule.maghrib))
        assertEquals(min(base.isha).minusMinutes(1), min(schedule.isha))
    }

    @Test
    fun `active prayer name reflects time of day`() {
        val schedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, settings)

        val preFajr = ZonedDateTime.of(2026, 8, 15, 1, 0, 0, 0, ZoneId.of(riyadh))
        val afterFajr = ZonedDateTime.of(2026, 8, 15, 5, 0, 0, 0, ZoneId.of(riyadh))
        val lateNight = ZonedDateTime.of(2026, 8, 15, 23, 30, 0, 0, ZoneId.of(riyadh))

        assertEquals("Isha", PrayerCalculator.getCurrentActivePrayer(schedule, preFajr))
        assertEquals("Fajr", PrayerCalculator.getCurrentActivePrayer(schedule, afterFajr))
        assertEquals("Isha", PrayerCalculator.getCurrentActivePrayer(schedule, lateNight))
    }
}