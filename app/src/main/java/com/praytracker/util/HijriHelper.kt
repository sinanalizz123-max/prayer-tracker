package com.praytracker.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

object HijriHelper {

    val MONTHS_EN = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    val MONTHS_AR = listOf(
        "مُحَرَّم", "صَفَر", "رَبِيع الأَوَّل", "رَبِيع الثَّانِي",
        "جُمَادَى الأُولَى", "جُمَادَى الآخِرَة", "رَجَب", "شَعْبَان",
        "رَمَضَان", "شَوَّال", "ذُو الْقَعْدَة", "ذُو الْحِجَّة"
    )

    val WEEKDAYS_EN = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val WEEKDAYS_AR = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")

    data class HijriDateInfo(
        val day: Int,
        val month: Int, // 1-indexed (1 to 12)
        val year: Int,
        val monthNameEn: String,
        val monthNameAr: String,
        val formattedEn: String,
        val formattedAr: String,
        val isRamadan: Boolean,
        val eventName: String? = null
    )

    data class HijriCalendarDay(
        val hijriDay: Int,
        val hijriMonth: Int,
        val hijriYear: Int,
        val gregorianDate: LocalDate,
        val dayOfWeek: DayOfWeek,
        val isToday: Boolean,
        val isWhiteDay: Boolean,
        val isFriday: Boolean,
        val eventName: String?
    )

    fun toArabicNumbers(input: String): String {
        val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val builder = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                builder.append(arabicChars[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun formatNumber(number: Int, useArabic: Boolean): String {
        return if (useArabic) toArabicNumbers(number.toString()) else number.toString()
    }

    fun getIslamicEvent(hijriMonth: Int, hijriDay: Int): String? {
        return when {
            hijriMonth == 1 && hijriDay == 1 -> "Islamic New Year"
            hijriMonth == 1 && hijriDay == 9 -> "Tasu'a"
            hijriMonth == 1 && hijriDay == 10 -> "Day of Ashura"
            hijriMonth == 3 && hijriDay == 12 -> "Mawlid an-Nabi"
            hijriMonth == 7 && hijriDay == 27 -> "Isra & Mi'raj"
            hijriMonth == 8 && hijriDay == 15 -> "Laylat al-Bara'at (Nisf Sha'ban)"
            hijriMonth == 9 && hijriDay == 1 -> "1st of Ramadan"
            hijriMonth == 9 && hijriDay == 27 -> "Laylat al-Qadr (Estimated)"
            hijriMonth == 9 && hijriDay in 21..29 && hijriDay % 2 != 0 -> "Odd Night (Laylat al-Qadr)"
            hijriMonth == 10 && hijriDay == 1 -> "Eid al-Fitr (Day 1)"
            hijriMonth == 10 && hijriDay == 2 -> "Eid al-Fitr (Day 2)"
            hijriMonth == 10 && hijriDay == 3 -> "Eid al-Fitr (Day 3)"
            hijriMonth == 12 && hijriDay == 1 -> "Start of 10 Days of Dhu al-Hijjah"
            hijriMonth == 12 && hijriDay == 8 -> "Day of Tarwiyah"
            hijriMonth == 12 && hijriDay == 9 -> "Day of Arafah"
            hijriMonth == 12 && hijriDay == 10 -> "Eid al-Adha (Day 1)"
            hijriMonth == 12 && hijriDay in 11..13 -> "Days of Tashreeq"
            hijriDay in 13..15 && hijriMonth != 9 -> "White Day (Ayyam al-Beed)"
            else -> null
        }
    }

    fun getHijriDate(gregorianDate: LocalDate, adjustmentDays: Int, useArabicNumerals: Boolean = false): HijriDateInfo {
        return try {
            val hijrahDate = HijrahDate.from(gregorianDate)
                .plus(adjustmentDays.toLong(), ChronoUnit.DAYS)

            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val year = hijrahDate.get(ChronoField.YEAR)

            val monthIndex = (month - 1).coerceIn(0, 11)
            val monthNameEn = MONTHS_EN[monthIndex]
            val monthNameAr = MONTHS_AR[monthIndex]

            val displayDay = if (useArabicNumerals) toArabicNumbers(day.toString()) else day.toString()
            val displayYear = if (useArabicNumerals) toArabicNumbers(year.toString()) else year.toString()

            val arDay = toArabicNumbers(day.toString())
            val arYear = toArabicNumbers(year.toString())
            val event = getIslamicEvent(month, day)

            HijriDateInfo(
                day = day,
                month = month,
                year = year,
                monthNameEn = monthNameEn,
                monthNameAr = monthNameAr,
                formattedEn = "$displayDay $monthNameEn $displayYear AH",
                formattedAr = "$arDay $monthNameAr $arYear هـ",
                isRamadan = (month == 9),
                eventName = event
            )
        } catch (e: Exception) {
            val event = getIslamicEvent(1, 1)
            HijriDateInfo(
                day = 1,
                month = 1,
                year = 1448,
                monthNameEn = "Muharram",
                monthNameAr = "مُحَرَّم",
                formattedEn = "1 Muharram 1448 AH",
                formattedAr = "١ مُحَرَّم ١٤٤٨ هـ",
                isRamadan = false,
                eventName = event
            )
        }
    }

    /**
     * Converts a Hijri year, month, day to corresponding Gregorian LocalDate
     * with adjustmentDays taken into account.
     */
    fun toGregorianDate(hijriYear: Int, hijriMonth: Int, hijriDay: Int, adjustmentDays: Int): LocalDate {
        return try {
            val hijrahDate = HijrahChronology.INSTANCE.date(hijriYear, hijriMonth, hijriDay)
            LocalDate.from(hijrahDate).minusDays(adjustmentDays.toLong())
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    /**
     * Retrieves all days of a target Hijri month, including length of month and day calculations.
     */
    fun getHijriMonthDays(
        hijriYear: Int,
        hijriMonth: Int,
        adjustmentDays: Int,
        todayGregorian: LocalDate = LocalDate.now()
    ): List<HijriCalendarDay> {
        val daysList = mutableListOf<HijriCalendarDay>()
        try {
            val sampleHijrah = HijrahChronology.INSTANCE.date(hijriYear, hijriMonth, 1)
            val lengthOfMonth = sampleHijrah.lengthOfMonth()

            for (day in 1..lengthOfMonth) {
                val greg = toGregorianDate(hijriYear, hijriMonth, day, adjustmentDays)
                val isToday = (greg == todayGregorian)
                val isWhiteDay = (day in 13..15)
                val isFriday = (greg.dayOfWeek == DayOfWeek.FRIDAY)
                val event = getIslamicEvent(hijriMonth, day)

                daysList.add(
                    HijriCalendarDay(
                        hijriDay = day,
                        hijriMonth = hijriMonth,
                        hijriYear = hijriYear,
                        gregorianDate = greg,
                        dayOfWeek = greg.dayOfWeek,
                        isToday = isToday,
                        isWhiteDay = isWhiteDay,
                        isFriday = isFriday,
                        eventName = event
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback for 30 days
            for (day in 1..30) {
                val greg = todayGregorian.plusDays((day - 1).toLong())
                daysList.add(
                    HijriCalendarDay(
                        hijriDay = day,
                        hijriMonth = hijriMonth,
                        hijriYear = hijriYear,
                        gregorianDate = greg,
                        dayOfWeek = greg.dayOfWeek,
                        isToday = (day == 1),
                        isWhiteDay = (day in 13..15),
                        isFriday = (greg.dayOfWeek == DayOfWeek.FRIDAY),
                        eventName = getIslamicEvent(hijriMonth, day)
                    )
                )
            }
        }
        return daysList
    }
}
