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

    @Test
    fun `all calculation methods produce an ordered schedule`() {
        for (method in 0..9) {
            val methodSettings = TestSettings(calculationMethod = method, locationName = "Makkah")
            val schedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, methodSettings)

            assertTrue("method $method: fajr before sunrise", schedule.fajr.isBefore(schedule.sunrise))
            assertTrue("method $method: sunrise before dhuhr", schedule.sunrise.isBefore(schedule.dhuhr))
            assertTrue("method $method: dhuhr before asr", schedule.dhuhr.isBefore(schedule.asr))
            assertTrue("method $method: asr before maghrib", schedule.asr.isBefore(schedule.maghrib))
            assertTrue("method $method: maghrib before isha", schedule.maghrib.isBefore(schedule.isha))
            assertEquals("method $method: schedule belongs to requested date", date, schedule.list.first().time.toLocalDate())
        }
    }

    @Test
    fun `hanafi asr is later than shafi asr`() {
        val shafi = PrayerCalculator.calculateSchedule(
            makkahLat, makkahLon, riyadh, date, TestSettings(madhab = 0, locationName = "Makkah")
        )
        val hanafi = PrayerCalculator.calculateSchedule(
            makkahLat, makkahLon, riyadh, date, TestSettings(madhab = 1, locationName = "Makkah")
        )

        assertTrue(hanafi.asr.isAfter(shafi.asr))
        val gapMinutes = java.time.temporal.ChronoUnit.MINUTES.between(shafi.asr, hanafi.asr)
        assertTrue("Hanafi Asr should lag Shafi Asr by 30-180 minutes, was $gapMinutes", gapMinutes in 30..180)
    }

    @Test
    fun `high latitude schedules stay ordered across rules and seasons`() {
        val latitude = 64.1466
        val longitude = -21.9426
        val zone = "Atlantic/Reykjavik"
        val dates = listOf(LocalDate.of(2026, 3, 21), LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21))

        for (date in dates) {
            for (rule in 0..2) {
                val schedule = PrayerCalculator.calculateSchedule(
                    latitude, longitude, zone, date, TestSettings(highLatitudeRule = rule, locationName = "Reykjavik")
                )
                assertTrue("rule=$rule date=$date", schedule.fajr.isBefore(schedule.sunrise))
                assertTrue("rule=$rule date=$date", schedule.sunrise.isBefore(schedule.dhuhr))
                assertTrue("rule=$rule date=$date", schedule.dhuhr.isBefore(schedule.asr))
                assertTrue("rule=$rule date=$date", schedule.asr.isBefore(schedule.maghrib))
                assertTrue("rule=$rule date=$date", schedule.maghrib.isBefore(schedule.isha))
            }
        }
    }

    @Test
    fun `high latitude location computes a full schedule`() {
        val schedule = PrayerCalculator.calculateSchedule(
            61.2181, -149.9003, "America/Anchorage", LocalDate.of(2026, 6, 21),
            TestSettings(highLatitudeRule = 1, locationName = "Anchorage")
        )
        assertEquals(LocalDate.of(2026, 6, 21), schedule.date)
        assertEquals(6, schedule.list.size)
    }

    @Test
    fun `schedule honors the requested timezone`() {
        val riyadhSchedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, "Asia/Riyadh", date, settings)
        val nySchedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, "America/New_York", date, settings)

        assertEquals(ZoneId.of("Asia/Riyadh"), riyadhSchedule.fajr.zone)
        assertEquals(ZoneId.of("America/New_York"), nySchedule.fajr.zone)

        val nearestDay = setOf(date, date.minusDays(1), date.plusDays(1))
        assertTrue("NY schedule must fall within one day of $date",
            nySchedule.list.any { it.time.toLocalDate() in nearestDay })

        val offsetDifferenceHours = Math.abs(
            nySchedule.fajr.offset.totalSeconds - riyadhSchedule.fajr.offset.totalSeconds
        ) / 3600.0
        assertTrue(offsetDifferenceHours >= 6)
    }

    @Test
    fun `next prayer crosses midnight into next day fajr`() {
        val now = ZonedDateTime.of(2026, 8, 15, 23, 45, 0, 0, ZoneId.of(riyadh))
        val next = PrayerCalculator.getNextPrayer(makkahLat, makkahLon, riyadh, now, settings)

        assertEquals("FAJR", next.name)
        assertEquals(LocalDate.of(2026, 8, 16), next.targetTime.toLocalDate())
        assertTrue(next.countdownMinutes in 240..420)
    }

    @Test
    fun `cached schedules produce identical next prayer results`() {
        val todaySchedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date, settings)
        val tomorrowSchedule = PrayerCalculator.calculateSchedule(makkahLat, makkahLon, riyadh, date.plusDays(1), settings)

        fun checkEquivalent(reference: PrayerCalculator.NextPrayerInfo, cached: PrayerCalculator.NextPrayerInfo, time: ZonedDateTime) {
            assertEquals("name at $time", reference.name, cached.name)

            assertEquals("formattedTime must match its target at $time",
                reference.targetTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US)),
                reference.formattedTime)

            val driftMillis = java.time.temporal.ChronoUnit.MILLIS.between(reference.targetTime, cached.targetTime)
            assertTrue("targetTime drift $driftMillis ms at $time", Math.abs(driftMillis) < 2000)

            val refTotal = (reference.countdownMinutes * 60) + reference.countdownSeconds
            val cachedTotal = (cached.countdownMinutes * 60) + cached.countdownSeconds
            assertTrue("countdown drift at $time: $refTotal s vs $cachedTotal s",
                Math.abs(refTotal - cachedTotal) <= 2)
        }

        var time = ZonedDateTime.of(2026, 8, 15, 0, 5, 0, 0, ZoneId.of(riyadh))
        val end = ZonedDateTime.of(2026, 8, 16, 0, 0, 0, 0, ZoneId.of(riyadh))

        var samplesChecked = 0
        while (time.isBefore(end)) {
            val reference = PrayerCalculator.getNextPrayer(makkahLat, makkahLon, riyadh, time, settings)
            val cached = PrayerCalculator.getNextPrayer(todaySchedule, tomorrowSchedule, time)
            checkEquivalent(reference, cached, time)
            time = time.plusMinutes(15)
            samplesChecked++
        }
        assertTrue(samplesChecked >= 90)

        for (time in listOf(
            ZonedDateTime.of(2026, 8, 15, 12, 30, 0, 456000000, ZoneId.of(riyadh)),
            ZonedDateTime.of(2026, 8, 15, 23, 59, 30, 0, ZoneId.of(riyadh))
        )) {
            val reference = PrayerCalculator.getNextPrayer(makkahLat, makkahLon, riyadh, time, settings)
            val cached = PrayerCalculator.getNextPrayer(todaySchedule, tomorrowSchedule, time)
            checkEquivalent(reference, cached, time)
        }
    }
}