package com.praytracker.hijri

import java.time.LocalDate
import kotlin.math.floor

/**
 * Hijri date conversion.
 *
 * Primary (in range 1300..1600 AH, i.e. from 1882 CE): the official Saudi
 * Umm al-Qura calendar (see [UmmAlQura]).
 *
 * Fallback (outside that range): a tabular (arithmetic) Islamic civil
 * calendar using a 30-year cycle (months alternate 29/30 days, leap years in
 * years 2,5,7,10,13,16,18,21,24,26,29 of each cycle). The tabular calendar may
 * differ from lunar observation by a day; a manual Hijri day adjustment is
 * available in the app settings.
 *
 * Umm al-Qura epoch anchor: 1 Muharram 1300 AH = 12 November 1882 CE
 * (proleptic Gregorian), matching the OpenJDK hijrah configuration.
 */
object HijriDateUtil {

    data class HijriDate(
        val year: Int,
        val month: Int,
        val day: Int,
    ) {
        val monthName: String get() = MONTH_NAMES[month - 1]

        override fun toString(): String = "$day $monthName $year"

        fun isRamadan(): Boolean = month == RAMADAN_MONTH
    }

    const val RAMADAN_MONTH = 9

    val MONTH_NAMES = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Thaniya", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah",
    )

    // ----------------------- Public API -----------------------

    fun fromGregorian(date: LocalDate): HijriDate {
        val uq = UmmAlQura.toHijri(date.year, date.monthValue, date.dayOfMonth)
        if (uq != null) return HijriDate(uq.first, uq.second, uq.third)
        return jdnToTabular(epochDayToJdn(date.toEpochDay()))
    }

    fun toGregorian(date: HijriDate): LocalDate {
        val uq = UmmAlQura.toGregorian(date.year, date.month, date.day)
        if (uq != null) {
            return try {
                LocalDate.of(uq.first, uq.second, uq.third)
            } catch (_: Exception) {
                LocalDate.ofEpochDay(0) // unreachable for valid range
            }
        }
        return LocalDate.ofEpochDay(jdnToEpochDay(tabularToJdn(date.year, date.month, date.day)))
    }

    fun monthLength(year: Int, month: Int): Int =
        if (year in UmmAlQura.START_YEAR..UmmAlQura.END_YEAR) {
            UmmAlQura.getDaysInMonth(year, month)
        } else {
            tabularMonthLength(year, month)
        }

    fun ramadanLength(year: Int): Int = monthLength(year, RAMADAN_MONTH)

    /** Day-of-Ramadan (1..length) and length for a Gregorian date, or null if not in Ramadan. */
    fun ramadanDay(date: LocalDate): Pair<Int, Int>? {
        val h = fromGregorian(date)
        if (!h.isRamadan()) return null
        return h.day to ramadanLength(h.year)
    }

    // ----------------------- Tabular fallback -----------------------

    // 1 Muharram 1 AH = JDN (noon based) 1948440 => proleptic Gregorian 622-07-19.
    private const val TABULAR_EPOCH_JDN = 1948440L

    private fun jdnToEpochDay(jdn: Long): Long = jdn - 2440588L

    private fun epochDayToJdn(epochDay: Long): Long = epochDay + 2440588L

    fun tabularIsLeapYear(year: Int): Boolean = ((11 * year + 14) % 30) < 11

    fun tabularMonthLength(year: Int, month: Int): Int = when {
        month == 1 -> 30
        month == 12 -> if (tabularIsLeapYear(year)) 30 else 29
        month % 2 == 1 -> 30
        else -> 29
    }

    fun tabularToJdn(year: Int, month: Int, day: Int): Long {
        var s = 0L
        var m = 1
        while (m < month) {
            s += tabularMonthLength(year, m)
            m++
        }
        var y = 1
        while (y < year) {
            s += if (tabularIsLeapYear(y)) 355 else 354
            y++
        }
        return TABULAR_EPOCH_JDN + s + day - 1
    }

    private fun jdnToTabular(jdnIn: Long): HijriDate {
        var jdn = jdnIn
        var year = ((30 * (jdn - TABULAR_EPOCH_JDN) + 10646) / 10631).toInt()
        if (year < 1) year = 1
        while (jdn < tabularToJdn(year, 1, 1)) year--
        while (jdn > tabularToJdn(year, 12, tabularMonthLength(year, 12))) year++
        var month = 1
        while (month <= 12) {
            if (jdn <= tabularToJdn(year, month, tabularMonthLength(year, month))) break
            month++
        }
        if (month > 12) month = 12
        val day = (jdn - tabularToJdn(year, month, 1) + 1).toInt()
        return HijriDate(year, month, day)
    }
}