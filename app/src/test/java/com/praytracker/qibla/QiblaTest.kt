package com.praytracker.qibla

import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaTest {

    @Test
    fun `jeddah bearing`() {
        val bearing = QiblaCalculator.bearing(21.4858, 39.1925)
        assertEquals(96.0, bearing, 1.0)
    }

    @Test
    fun `new york bearing`() {
        val bearing = QiblaCalculator.bearing(40.7128, -74.0060)
        assertEquals(58.0, bearing, 1.0)
    }

    @Test
    fun `london bearing`() {
        val bearing = QiblaCalculator.bearing(51.5074, -0.1278)
        assertEquals(119.0, bearing, 1.0)
    }

    @Test
    fun `tehran bearing`() {
        val bearing = QiblaCalculator.bearing(35.6892, 51.3890)
        assertEquals(218.0, bearing, 1.0)
    }

    @Test
    fun `jakarta bearing`() {
        val bearing = QiblaCalculator.bearing(-6.2088, 106.8456)
        assertEquals(295.0, bearing, 1.0)
    }
}