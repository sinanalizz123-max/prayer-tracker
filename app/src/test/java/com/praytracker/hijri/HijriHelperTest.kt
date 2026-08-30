package com.praytracker.hijri

import com.praytracker.util.HijriHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HijriHelperTest {

    @Test
    fun `known gregorian date converts to hijri`() {
        val info = HijriHelper.getHijriDate(LocalDate.of(2026, 8, 30), 0)
        assertEquals(17, info.day)
        assertEquals(3, info.month)
        assertEquals(1448, info.year)
        assertEquals("Rabi' al-Awwal", info.monthNameEn)
        assertFalse(info.isRamadan)
        assertNull(info.eventName)
        assertTrue(info.formattedEn.startsWith("17 Rabi' al-Awwal 1448"))
    }

    @Test
    fun `hijri adjustment shifts the date by one day`() {
        val base = HijriHelper.getHijriDate(LocalDate.of(2026, 8, 30), 0)
        val shifted = HijriHelper.getHijriDate(LocalDate.of(2026, 8, 30), 1)
        assertEquals(base.day + 1, shifted.day)
    }

    @Test
    fun `hijri to gregorian round trip`() {
        val greg = HijriHelper.toGregorianDate(1448, 3, 17, 0)
        assertEquals(LocalDate.of(2026, 8, 30), greg)
        val back = HijriHelper.getHijriDate(greg, 0)
        assertEquals(1448, back.year)
        assertEquals(3, back.month)
        assertEquals(17, back.day)
    }

    @Test
    fun `month days are 29 or 30 and start on the first`() {
        val days = HijriHelper.getHijriMonthDays(1448, 3, 0, LocalDate.of(2026, 8, 30))
        assertTrue("month length must be 29 or 30", days.size == 29 || days.size == 30)
        assertEquals(1, days.first().hijriDay)
        assertEquals(days.size, days.last().hijriDay)
        val gregorianDates = days.map { it.gregorianDate }
        assertEquals(gregorianDates.sorted(), gregorianDates)
    }

    @Test
    fun `friday flag matches weekday`() {
        val today = LocalDate.of(2026, 8, 30)
        val days = HijriHelper.getHijriMonthDays(1448, 3, 0, today)
        val friday = days.first { it.gregorianDate.dayOfWeek == DayOfWeek.FRIDAY }
        assertTrue(friday.isFriday)
        val nonFriday = days.first { it.gregorianDate.dayOfWeek != DayOfWeek.FRIDAY }
        assertFalse(nonFriday.isFriday)
    }

    @Test
    fun `today is marked in the current month`() {
        val days = HijriHelper.getHijriMonthDays(1448, 3, 0, LocalDate.of(2026, 8, 30))
        val today = days.first { it.isToday }
        assertEquals(LocalDate.of(2026, 8, 30), today.gregorianDate)
    }

    @Test
    fun `white days are 13 to 15`() {
        val days = HijriHelper.getHijriMonthDays(1448, 3, 0, LocalDate.of(2026, 8, 30))
        val whiteDays = days.filter { it.isWhiteDay }
        assertEquals(listOf(13, 14, 15), whiteDays.map { it.hijriDay })
    }

    @Test
    fun `arabic number conversion maps western digits`() {
        assertEquals("٢٠٢٤", HijriHelper.toArabicNumbers("2024"))
        assertEquals("٠", HijriHelper.toArabicNumbers("0"))
        assertEquals("١٤٤٨-٣-١٧", HijriHelper.toArabicNumbers("1448-3-17"))
    }

    @Test
    fun `islamic event lookup returns expected events`() {
        assertEquals("Islamic New Year", HijriHelper.getIslamicEvent(1, 1))
        assertEquals("Day of Arafah", HijriHelper.getIslamicEvent(12, 9))
        assertEquals("Eid al-Adha (Day 1)", HijriHelper.getIslamicEvent(12, 10))
        assertEquals("1st of Ramadan", HijriHelper.getIslamicEvent(9, 1))
        assertEquals("Eid al-Fitr (Day 1)", HijriHelper.getIslamicEvent(10, 1))
        assertNull(HijriHelper.getIslamicEvent(5, 17))
    }

    @Test
    fun `month name lists contain twelve entries`() {
        assertEquals(12, HijriHelper.MONTHS_EN.size)
        assertEquals(12, HijriHelper.MONTHS_AR.size)
        assertNotNull(HijriHelper.WEEKDAYS_EN[5])
    }
}