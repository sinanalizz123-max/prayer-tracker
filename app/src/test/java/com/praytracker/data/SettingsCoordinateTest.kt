package com.praytracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCoordinateTest {

    @Test
    fun `no stored precision value falls back to legacy float`() {
        assertEquals(0.0, resolveStoredCoordinate(null, 0.0f), 0.0)
        assertEquals(51.5074f.toDouble(), resolveStoredCoordinate(null, 51.5074f), 0.0)
    }

    @Test
    fun `stored precise string wins over legacy float`() {
        val resolved = resolveStoredCoordinate("51.5073884", 51.5074f)
        assertEquals(51.5073884, resolved, 0.0)
    }

    @Test
    fun `invalid precise string falls back to legacy float`() {
        assertEquals(39.8262f.toDouble(), resolveStoredCoordinate("not-a-number", 39.8262f), 0.0)
    }

    @Test
    fun `stored precise string round trips at full precision`() {
        val precise = 123.456789012345
        val resolved = resolveStoredCoordinate(precise.toString(), 123.4567f)
        assertEquals(precise, resolved, 0.0)
    }

    @Test
    fun `float persistence loses precision that double persistence keeps`() {
        val precise = 123.456789012345
        val throughFloat = precise.toFloat().toDouble()
        assertNotEquals("float round-trip must lose precision", precise, throughFloat, 0.0)
        assertTrue("float error ${Math.abs(precise - throughFloat)} must exceed double error",
            Math.abs(precise - throughFloat) > 1e-9)
    }
}