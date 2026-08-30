package com.praytracker.data.repo

import com.praytracker.data.settings.Settings
import com.praytracker.data.settings.SettingsRepository
import com.praytracker.prayer.CalcMethod
import com.praytracker.prayer.DailyPrayerTimes
import com.praytracker.prayer.GeoLocation
import com.praytracker.prayer.HighLatRule
import com.praytracker.prayer.Madhab
import com.praytracker.prayer.Prayer
import com.praytracker.prayer.PrayerTimesCalculator
import java.time.LocalDate
import java.time.ZoneId

/**
 * Computes prayer times from persisted settings. LocalDate refers to the
 * Gregorian local date; all calculation happens in the device local time zone.
 */
class PrayerTimesRepository(private val settingsRepository: SettingsRepository) {

    suspend fun timesFor(date: LocalDate): DailyPrayerTimes {
        val settings = settingsRepository.snapshot()
        return compute(date, settings)
    }

    fun compute(date: LocalDate, settings: Settings): DailyPrayerTimes {
        val method = enumValueOfOrNull<CalcMethod>(settings.calcMethod) ?: CalcMethod.MWL
        val madhab = enumValueOfOrNull<Madhab>(settings.madhab) ?: Madhab.SHAFI
        val highLat = enumValueOfOrNull<HighLatRule>(settings.highLatRule) ?: HighLatRule.NIGHT_MIDDLE
        val tzOffsetHours = date.atStartOfDay(ZoneId.systemDefault()).offset.totalSeconds / 3600.0
        return PrayerTimesCalculator.compute(
            location = GeoLocation(settings.latitude, settings.longitude),
            date = date,
            method = method,
            madhab = madhab,
            highLats = highLat,
            tzOffsetHours = tzOffsetHours,
            adjustments = settings.adjustments,
        )
    }

    /** Next prayer on the given day at `now`, accounting for times that fall on the previous day. */
    suspend fun nextPrayer(now: java.time.LocalDateTime): Pair<Prayer, java.time.LocalDateTime>? {
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val candidates = mutableListOf<Pair<Prayer, java.time.LocalDateTime>>()
        Prayer.ORDER.forEach { p ->
            timesFor(today).localDateTime(p)?.let { if (!it.isBefore(now)) candidates.add(p to it) }
        }
        Prayer.ORDER.forEach { p ->
            timesFor(tomorrow).localDateTime(p)?.let { candidates.add(p to it) }
        }
        return candidates.minByOrNull { it.second }
    }
}

private inline fun <reified T : Enum<T>> enumValueOfOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }