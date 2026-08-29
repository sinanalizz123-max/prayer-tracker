package com.praytracker.hijri

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriTest {

    @Test
    fun `ramadan start matches aladhan Umm al-Qura`() {
        val h = HijriDateUtil.fromGregorian(LocalDate.of(2024, 3, 11))
        assertEquals(1445, h.year)
        assertEquals(HijriDateUtil.RAMADAN_MONTH, h.month)
        assertEquals(1, h.day)
    }

    @Test
    fun `june end of ramadan`() {
        val h = HijriDateUtil.fromGregorian(LocalDate.of(2024, 4, 9))
        assertEquals(1445, h.year)
        assertEquals(9, h.month)
        assertEquals(30, h.day)
    }

    @Test
    fun `modern table date`() {
        val h = HijriDateUtil.fromGregorian(LocalDate.of(2024, 12, 21))
        assertEquals(1446, h.year)
        assertEquals(6, h.month) // Jumada al-Thaniya
        assertEquals(20, h.day)
    }

    @Test
    fun `next ramadan`() {
        val h = HijriDateUtil.fromGregorian(LocalDate.of(2025, 3, 1))
        assertEquals(1446, h.year)
        assertEquals(9, h.month)
        assertEquals(1, h.day)
    }

    @Test
    fun `ramadan day helpers`() {
        val start = HijriDateUtil.ramadanDay(LocalDate.of(2024, 3, 11))
        assertNotNull(start)
        assertEquals(1, start!!.first)
        assertEquals(30, start.second)

        val end = HijriDateUtil.ramadanDay(LocalDate.of(2024, 4, 9))
        assertEquals(30, end!!.first)

        assertNull(HijriDateUtil.ramadanDay(LocalDate.of(2024, 5, 1)))
    }

    @Test
    fun `round trip`() {
        val d = LocalDate.of(2026, 8, 30)
        val h = HijriDateUtil.fromGregorian(d)
        assertNotNull(h)
        assertEquals(d, HijriDateUtil.toGregorian(h))
    }

    @Test
    fun `anchor epoch`() {
        val h = HijriDateUtil.fromGregorian(LocalDate.of(1882, 11, 12))
        assertEquals(1300, h.year)
        assertEquals(1, h.month)
        assertEquals(1, h.day)
    }

    @Test
    fun `month length consistent`() {
        for (year in 1440..1450) {
            val total = (1..12).sumOf { HijriDateUtil.monthLength(year, it) }
            assertTrue(total == 354 || total == 355)
        }
    }

    @Test
    fun `tabular fallback outside range`() {
        val h = HijriDateUtil.fromGregorian(LocalDate.of(1880, 1, 1))
        assertNotNull(h)
        assertTrue(h.year < 1300)
    }
}