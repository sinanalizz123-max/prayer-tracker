package com.praytracker.util

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.praytracker.data.PrayerSettings
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

object PrayerCalculator {

    val CALCULATION_METHOD_NAMES = listOf(
        "Muslim World League",
        "ISNA (North America)",
        "Egyptian General Authority",
        "Umm al-Qura (Makkah)",
        "University of Islamic Sciences, Karachi",
        "Kuwait",
        "Qatar",
        "Majlis Ugama Islam Singapura",
        "Dubai",
        "Moonsighting Committee Worldwide"
    )

    val MADHAB_NAMES = listOf(
        "Shafi'i, Maliki, Hanbali (Standard)",
        "Hanafi (Later Asr shadow)"
    )

    val HIGH_LATITUDE_RULE_NAMES = listOf(
        "Middle of the Night",
        "One Seventh of the Night",
        "Twilight Angle"
    )

    data class PrayerSchedule(
        val date: LocalDate,
        val formattedGregorianDate: String,
        val hijriDateEn: String,
        val hijriDateAr: String,
        val locationName: String,
        val fajr: ZonedDateTime,
        val sunrise: ZonedDateTime,
        val dhuhr: ZonedDateTime,
        val asr: ZonedDateTime,
        val maghrib: ZonedDateTime,
        val isha: ZonedDateTime,
        val list: List<PrayerItem>
    )

    data class PrayerItem(
        val name: String,
        val type: String, // "FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA"
        val time: ZonedDateTime,
        val formattedTime: String,
        val isSunrise: Boolean = false
    )

    data class NextPrayerInfo(
        val name: String,
        val formattedTime: String,
        val countdownMinutes: Long,
        val countdownSeconds: Long,
        val targetTime: ZonedDateTime
    )

    fun calculateSchedule(
        lat: Double,
        lon: Double,
        timezoneId: String,
        localDate: LocalDate,
        settings: PrayerSettings
    ): PrayerSchedule {
        val zoneId = try { ZoneId.of(timezoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        
        val coordinates = Coordinates(lat, lon)
        val dateComponents = DateComponents(localDate.year, localDate.monthValue, localDate.dayOfMonth)
        
        val params = getAdhanParameters(settings)
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)

        // During polar day/night the sun stays above/below the horizon the whole
        // day (e.g. the Arctic summer), so the adhan library leaves some prayer
        // boundaries undefined (null). Falling back to monotonically increasing
        // anchors around local solar noon keeps the schedule usable and ordered
        // instead of crashing. When every boundary is defined nothing changes.
        val fallback = fallbackBoundaries(zoneId, localDate)
        val baseFajr = rawToZoned(prayerTimes.fajr, zoneId) ?: fallback.fajr
        val baseSunrise = rawToZoned(prayerTimes.sunrise, zoneId) ?: fallback.sunrise
        val baseDhuhr = rawToZoned(prayerTimes.dhuhr, zoneId) ?: fallback.dhuhr
        val baseAsr = rawToZoned(prayerTimes.asr, zoneId) ?: fallback.asr
        val baseMaghrib = rawToZoned(prayerTimes.maghrib, zoneId) ?: fallback.maghrib
        val baseIsha = rawToZoned(prayerTimes.isha, zoneId) ?: fallback.isha

        // Apply custom manual offsets
        val adjFajr = baseFajr.plusMinutes(settings.adjustmentFajr.toLong())
        val adjSunrise = baseSunrise
        val adjDhuhr = baseDhuhr.plusMinutes(settings.adjustmentDhuhr.toLong())
        val adjAsr = baseAsr.plusMinutes(settings.adjustmentAsr.toLong())
        val adjMaghrib = baseMaghrib.plusMinutes(settings.adjustmentMaghrib.toLong())
        val adjIsha = baseIsha.plusMinutes(settings.adjustmentIsha.toLong())

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        val items = listOf(
            PrayerItem("Fajr", "FAJR", adjFajr, adjFajr.format(timeFormatter)),
            PrayerItem("Sunrise", "SUNRISE", adjSunrise, adjSunrise.format(timeFormatter), isSunrise = true),
            PrayerItem("Dhuhr", "DHUHR", adjDhuhr, adjDhuhr.format(timeFormatter)),
            PrayerItem("Asr", "ASR", adjAsr, adjAsr.format(timeFormatter)),
            PrayerItem("Maghrib", "MAGHRIB", adjMaghrib, adjMaghrib.format(timeFormatter)),
            PrayerItem("Isha", "ISHA", adjIsha, adjIsha.format(timeFormatter))
        )

        val hijri = HijriHelper.getHijriDate(localDate, settings.hijriAdjustment)

        val gregFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
        val formattedGregorianDate = localDate.format(gregFormatter)

        return PrayerSchedule(
            date = localDate,
            formattedGregorianDate = formattedGregorianDate,
            hijriDateEn = hijri.formattedEn,
            hijriDateAr = hijri.formattedAr,
            locationName = settings.locationName,
            fajr = adjFajr,
            sunrise = adjSunrise,
            dhuhr = adjDhuhr,
            asr = adjAsr,
            maghrib = adjMaghrib,
            isha = adjIsha,
            list = items
        )
    }

    fun getNextPrayer(
        lat: Double,
        lon: Double,
        timezoneId: String,
        now: ZonedDateTime,
        settings: PrayerSettings
    ): NextPrayerInfo {
        val today = now.toLocalDate()
        val todaySchedule = calculateSchedule(lat, lon, timezoneId, today, settings)
        val tomorrowSchedule = calculateSchedule(lat, lon, timezoneId, today.plusDays(1), settings)
        return getNextPrayer(todaySchedule, tomorrowSchedule, now)
    }

    /**
     * Countdown logic against already-calculated today/tomorrow schedules. Cheap
     * enough to run every second while the app is on screen; callers are expected
     * to keep the two schedules cached and rebuild them only when the inputs
     * (date, location, timezone, method, madhab, high-latitude rule, offsets) change.
     */
    fun getNextPrayer(
        todaySchedule: PrayerSchedule,
        tomorrowSchedule: PrayerSchedule,
        now: ZonedDateTime
    ): NextPrayerInfo {
        val prospectivePrayers = listOf(
            Pair("FAJR", todaySchedule.fajr),
            Pair("DHUHR", todaySchedule.dhuhr),
            Pair("ASR", todaySchedule.asr),
            Pair("MAGHRIB", todaySchedule.maghrib),
            Pair("ISHA", todaySchedule.isha),
            Pair("FAJR", tomorrowSchedule.fajr)
        )

        var nextPrayer: Pair<String, ZonedDateTime>? = null
        for (prayer in prospectivePrayers) {
            if (prayer.second.isAfter(now)) {
                nextPrayer = prayer
                break
            }
        }

        val selected = nextPrayer ?: Pair("FAJR", tomorrowSchedule.fajr)

        val target = selected.second
        val totalSeconds = ChronoUnit.SECONDS.between(now, target).coerceAtLeast(0)
        val countdownMinutes = totalSeconds / 60
        val countdownSeconds = totalSeconds % 60

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

        return NextPrayerInfo(
            name = selected.first,
            formattedTime = target.format(timeFormatter),
            countdownMinutes = countdownMinutes,
            countdownSeconds = countdownSeconds,
            targetTime = target
        )
    }

    fun getCurrentActivePrayer(schedule: PrayerSchedule, now: ZonedDateTime): String {
        return when {
            now.isBefore(schedule.fajr) -> "Isha"
            now.isBefore(schedule.sunrise) -> "Fajr"
            now.isBefore(schedule.dhuhr) -> "Sunrise"
            now.isBefore(schedule.asr) -> "Dhuhr"
            now.isBefore(schedule.maghrib) -> "Asr"
            now.isBefore(schedule.isha) -> "Maghrib"
            else -> "Isha"
        }
    }

    private fun toZonedDateTime(date: Date, zoneId: ZoneId): ZonedDateTime {
        return ZonedDateTime.ofInstant(date.toInstant(), zoneId)
    }

    private fun rawToZoned(date: Date?, zoneId: ZoneId): ZonedDateTime? {
        return date?.let { toZonedDateTime(it, zoneId) }
    }

    private fun fallbackBoundaries(zoneId: ZoneId, localDate: LocalDate): solarBoundaries {
        val start = localDate.atStartOfDay(zoneId)
        return solarBoundaries(
            fajr = start.plusMinutes(1),
            sunrise = start.plusMinutes(2),
            dhuhr = localDate.atTime(12, 0).atZone(zoneId),
            asr = localDate.atTime(12, 1).atZone(zoneId),
            maghrib = localDate.atTime(12, 2).atZone(zoneId),
            isha = localDate.atTime(12, 3).atZone(zoneId)
        )
    }

    private data class solarBoundaries(
        val fajr: ZonedDateTime,
        val sunrise: ZonedDateTime,
        val dhuhr: ZonedDateTime,
        val asr: ZonedDateTime,
        val maghrib: ZonedDateTime,
        val isha: ZonedDateTime
    )

    private fun getAdhanParameters(settings: PrayerSettings): CalculationParameters {
        val params = when (settings.calculationMethod) {
            0 -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            1 -> CalculationMethod.NORTH_AMERICA.parameters
            2 -> CalculationMethod.EGYPTIAN.parameters
            3 -> CalculationMethod.UMM_AL_QURA.parameters
            4 -> CalculationMethod.KARACHI.parameters
            5 -> CalculationMethod.KUWAIT.parameters
            6 -> CalculationMethod.QATAR.parameters
            7 -> CalculationMethod.SINGAPORE.parameters
            8 -> CalculationMethod.DUBAI.parameters
            9 -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        }

        params.madhab = when (settings.madhab) {
            1 -> Madhab.HANAFI
            else -> Madhab.SHAFI
        }

        params.highLatitudeRule = when (settings.highLatitudeRule) {
            1 -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
            2 -> HighLatitudeRule.TWILIGHT_ANGLE
            else -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        }

        return params
    }
}
