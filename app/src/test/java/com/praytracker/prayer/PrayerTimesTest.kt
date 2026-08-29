package com.praytracker.prayer

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerTimesTest {

    private fun fmt(min: Double): String {
        val m = Math.round(min)
        return "%02d:%02d".format(m / 60, m % 60)
    }

    private fun times(
        lat: Double,
        lng: Double,
        date: LocalDate,
        method: CalcMethod,
        madhab: Madhab = Madhab.SHAFI,
        highLats: HighLatRule = HighLatRule.NIGHT_MIDDLE,
        tz: Double,
        adjustments: Map<Prayer, Int> = emptyMap(),
    ): DailyPrayerTimes = PrayerTimesCalculator.compute(
        location = GeoLocation(lat, lng),
        date = date,
        method = method,
        madhab = madhab,
        highLats = highLats,
        tzOffsetHours = tz,
        adjustments = adjustments,
    )

    @Test
    fun `mecca matches aladhan MWL`() {
        val t = times(21.422487, 39.826206, LocalDate.of(2024, 3, 11), CalcMethod.MWL, tz = 3.0)
        assertEquals("05:19", fmt(t.fajr!!.hour * 60.0 + t.fajr.minute))
        assertEquals("06:33", fmt(t.sunrise!!.hour * 60.0 + t.sunrise.minute))
        assertEquals("12:31", fmt(t.dhuhr!!.hour * 60.0 + t.dhuhr.minute))
        assertEquals("15:54", fmt(t.asr!!.hour * 60.0 + t.asr.minute))
        assertEquals("18:29", fmt(t.maghrib!!.hour * 60.0 + t.maghrib.minute))
        assertEquals("19:38", fmt(t.isha!!.hour * 60.0 + t.isha.minute))
    }

    @Test
    fun `new york matches aladhan ISNA`() {
        val t = times(40.7128, -74.0060, LocalDate.of(2024, 12, 21), CalcMethod.ISNA, tz = -5.0)
        assertEquals("05:54", fmt(t.fajr!!.hour * 60.0 + t.fajr.minute))
        assertEquals("07:17", fmt(t.sunrise!!.hour * 60.0 + t.sunrise.minute))
        assertEquals("11:54", fmt(t.dhuhr!!.hour * 60.0 + t.dhuhr.minute))
        assertEquals("14:14", fmt(t.asr!!.hour * 60.0 + t.asr.minute))
        assertEquals("16:32", fmt(t.maghrib!!.hour * 60.0 + t.maghrib.minute))
        assertEquals("17:54", fmt(t.isha!!.hour * 60.0 + t.isha.minute))
    }

    @Test
    fun `new york hanafi asr`() {
        val t = times(40.7128, -74.0060, LocalDate.of(2024, 12, 21), CalcMethod.ISNA, Madhab.HANAFI, tz = -5.0)
        assertEquals("14:51", fmt(t.asr!!.hour * 60.0 + t.asr.minute))
    }

    @Test
    fun `makkah method uses isha 90 minutes`() {
        val t = times(21.422487, 39.826206, LocalDate.of(2024, 3, 11), CalcMethod.MAKKAH, tz = 3.0)
        val maghribMin = t.maghrib!!.hour * 60.0 + t.maghrib.minute
        val ishaMin = t.isha!!.hour * 60.0 + t.isha.minute
        assertEquals(maghribMin + 90.0, ishaMin, 1.0)
    }

    @Test
    fun `london high latitude isha rolls to next day`() {
        val t = times(51.5074, -0.1278, LocalDate.of(2024, 6, 21), CalcMethod.MWL, tz = 1.0)
        // With NIGHT_MIDDLE, Isha (~25h raw) falls after midnight => 22 June 01:0x
        val ishaDt = t.localDateTime(Prayer.ISHA)!!
        assertEquals(LocalDate.of(2024, 6, 22), ishaDt.toLocalDate())
        assertEquals(1, ishaDt.hour)
        assert(ishaDt.minute in 0..5)
    }

    @Test
    fun `adjustments shift times`() {
        var t = times(40.7128, -74.0060, LocalDate.of(2024, 12, 21), CalcMethod.ISNA, tz = -5.0)
        val base = t.time(Prayer.DHUHR)!!
        t = times(
            40.7128, -74.0060, LocalDate.of(2024, 12, 21), CalcMethod.ISNA,
            tz = -5.0, adjustments = mapOf(Prayer.DHUHR to 5),
        )
        assertEquals(base.plusMinutes(5), t.time(Prayer.DHUHR))
    }

    @Test
    fun `next day boundary clean`() {
        val today = times(21.422487, 39.826206, LocalDate.of(2024, 3, 11), CalcMethod.MWL, tz = 3.0)
        val tomorrow = times(21.422487, 39.826206, LocalDate.of(2024, 3, 12), CalcMethod.MWL, tz = 3.0)
        assert(today.date == LocalDate.of(2024, 3, 11))
        assert(tomorrow.date == LocalDate.of(2024, 3, 12))
    }
}