package com.praytracker.qibla

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.PI

/**
 * Qibla direction (bearing to the Kaaba, Mecca).
 * Kaaba coordinates approximate 21.4225 E lat, 39.8262 E lon.
 */
object QiblaCalculator {

    val KAABA_LATITUDE = 21.4225
    val KAABA_LONGITUDE = 39.8262

    /**
     * Initial bearing (degrees, 0..360, clockwise from true north) from
     * [userLatitude]/[userLongitude] toward the Kaaba.
     */
    fun bearing(userLatitude: Double, userLongitude: Double): Double {
        val phi1 = Math.toRadians(userLatitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val dLambda = Math.toRadians(KAABA_LONGITUDE - userLongitude)

        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        var result = Math.toDegrees(atan2(y, x))
        if (result < 0) result += 360.0
        return result
    }

    /**
     * Signed angle (in degrees, -180..180) by which the compass should rotate
     * relative to the device azimuth to point at the qibla.
     */
    fun relativeAngle(qiblaBearing: Double, deviceAzimuthDegrees: Double): Double {
        var diff = (qiblaBearing - deviceAzimuthDegrees) % 360.0
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    /** Approximate great-circle distance in kilometers. */
    fun distanceKm(userLatitude: Double, userLongitude: Double): Double {
        val r = 6371.0
        val dLat = abs(Math.toRadians(KAABA_LATITUDE - userLatitude))
        val dLon = Math.toRadians(KAABA_LONGITUDE - userLongitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(userLatitude)) * cos(KAABA_LATITUDE * PI / 180.0) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}