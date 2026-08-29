package com.praytracker.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToLong

data class GeoLocation(val latitude: Double, val longitude: Double)

enum class Madhab(val displayName: String) {
    SHAFI("Shafi'i"),
    HANAFI("Hanafi"),
}

enum class HighLatRule(val displayName: String) {
    NIGHT_MIDDLE("Middle of the night"),
    ANGLE_BASED("Angle-based"),
    ONE_SEVENTH("One-seventh"),
    NONE("No adjustment"),
}

data class DailyPrayerTimes(
    val date: LocalDate,
    val fajr: LocalTime?,
    val sunrise: LocalTime?,
    val dhuhr: LocalTime?,
    val asr: LocalTime?,
    val maghrib: LocalTime?,
    val isha: LocalTime?,
    internal val rawHours: Map<Prayer, Double> = emptyMap(),
) {
    fun time(prayer: Prayer): LocalTime? = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    /** Local date-time for a prayer, rolling past-midnight prayers to the following day. */
    fun localDateTime(prayer: Prayer): LocalDateTime? {
        val t = time(prayer) ?: return null
        var result = date.atTime(t)
        val raw = rawHours[prayer]
        if (raw != null && raw >= 24.0) {
            result = result.plusDays(1)
        }
        return result
    }
}

object PrayerTimesCalculator {

    fun compute(
        location: GeoLocation,
        date: LocalDate,
        method: CalcMethod,
        madhab: Madhab,
        highLats: HighLatRule,
        tzOffsetHours: Double,
        adjustments: Map<Prayer, Int> = emptyMap(),
    ): DailyPrayerTimes {
        val params = PrayTimesEngine.Params(
            fajrAngle = method.fajrAngle,
            ishaValue = method.ishaAngle ?: (method.ishaMinutes ?: 17.0).toDouble(),
            ishaIsMinutes = method.ishaAngle == null,
            maghribValue = method.maghribAngle ?: method.maghribMinutes?.toDouble() ?: 0.0,
            maghribIsMinutes = method.maghribAngle == null,
            midNightJafari = method.jafariMidnight,
        )

        val offsets = adjustments.entries
            .filter { it.value != 0 }
            .associate { (p, m) -> p.displayName.lowercase() to m }

        val raw = PrayTimesEngine.compute(
            latitude = location.latitude,
            longitude = location.longitude,
            elevationMeters = 0.0,
            tzOffsetHours = tzOffsetHours,
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            params = params,
            asrParam = madhab.name,
            highLats = when (highLats) {
                HighLatRule.NIGHT_MIDDLE -> PrayTimesEngine.HighLats.NIGHT_MIDDLE
                HighLatRule.ANGLE_BASED -> PrayTimesEngine.HighLats.ANGLE_BASED
                HighLatRule.ONE_SEVENTH -> PrayTimesEngine.HighLats.ONE_SEVENTH
                HighLatRule.NONE -> PrayTimesEngine.HighLats.NONE
            },
            offsetsMinutes = offsets,
        )

        fun toLocalTime(rawHour: Double): LocalTime? =
            if (rawHour.isNaN()) null else minuteOfDayToLocalTime(rawHour)

        val rawHours = mapOfNotNull(
            Prayer.FAJR to raw.fajr,
            Prayer.DHUHR to raw.dhuhr,
            Prayer.ASR to raw.asr,
            Prayer.MAGHRIB to raw.maghrib,
            Prayer.ISHA to raw.isha,
        )

        return DailyPrayerTimes(
            date = date,
            fajr = toLocalTime(raw.fajr),
            sunrise = toLocalTime(raw.sunrise),
            dhuhr = toLocalTime(raw.dhuhr),
            asr = toLocalTime(raw.asr),
            maghrib = toLocalTime(raw.maghrib),
            isha = toLocalTime(raw.isha),
            rawHours = rawHours,
        )
    }

    private fun minuteOfDayToLocalTime(rawHour: Double): LocalTime {
        val totalMinutes = (rawHour * 60.0).roundToLong()
        val normalized = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        return LocalTime.ofSecondOfDay(normalized * 60)
    }

    private fun mapOfNotNull(vararg pairs: Pair<Prayer, Double>): Map<Prayer, Double> =
        pairs.filter { !it.second.isNaN() }.toMap()
}