package com.praytracker.prayer

import kotlin.math.asin
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Faithful Kotlin port of the PrayTimes.org calculation engine (ver 2.5).
 *
 * PrayTimes.js: Prayer Times Calculator (ver 2.5)
 * Copyright (C) 2007-2017 PrayTimes.org
 * Developer: Hamid Zarrabi-Zadeh
 * License: GNU LGPL v3.0
 * Manual & formulas: http://praytimes.org/manual , http://praytimes.org/calculation
 *
 * Math and control flow mirror the original JavaScript so results match the
 * published, widely-verified algorithm.
 */
internal object PrayTimesEngine {

    enum class HighLats { NONE, NIGHT_MIDDLE, ANGLE_BASED, ONE_SEVENTH }

    data class RawTimes(
        var imsak: Double = Double.NaN,
        var fajr: Double = Double.NaN,
        var sunrise: Double = Double.NaN,
        var dhuhr: Double = Double.NaN,
        var asr: Double = Double.NaN,
        var sunset: Double = Double.NaN,
        var maghrib: Double = Double.NaN,
        var isha: Double = Double.NaN,
        var midnight: Double = Double.NaN,
    )

    data class Params(
        val fajrAngle: Double,
        val ishaValue: Double,
        val ishaIsMinutes: Boolean = false,
        val maghribValue: Double = 0.0,
        val maghribIsMinutes: Boolean = true,
        val imsakValue: Double = 10.0,
        val imsakIsMinutes: Boolean = true,
        val midNightJafari: Boolean = false,
    )

    // ----------------------- Degree-based math -----------------------
    private fun dtr(d: Double): Double = Math.toRadians(d)
    private fun rtd(r: Double): Double = Math.toDegrees(r)

    private fun sin(d: Double): Double = kotlin.math.sin(dtr(d))
    private fun cos(d: Double): Double = kotlin.math.cos(dtr(d))
    private fun tan(d: Double): Double = kotlin.math.tan(dtr(d))
    private fun arcsin(x: Double): Double = rtd(asin(x))
    private fun arccos(x: Double): Double = rtd(acos(x))
    private fun arccot(x: Double): Double = rtd(atan(1.0 / x))
    private fun arctan2(y: Double, x: Double): Double = rtd(atan2(y, x))

    private fun fixAngle(a: Double): Double = fix(a, 360.0)
    private fun fixHour(a: Double): Double = fix(a, 24.0)

    private fun fix(aIn: Double, b: Double): Double {
        var a = aIn
        a -= b * floor(a / b)
        return if (a < 0) a + b else a
    }

    // ----------------------- State -----------------------
    private var lat = 0.0
    private var lng = 0.0
    private var elv = 0.0
    private var timeZone = 0.0
    private var jDate = 0.0

    // ----------------------- Main entry -----------------------
    fun compute(
        latitude: Double,
        longitude: Double,
        elevationMeters: Double,
        tzOffsetHours: Double,
        year: Int,
        month: Int,
        day: Int,
        params: Params,
        asrParam: String,
        highLats: HighLats,
        offsetsMinutes: Map<String, Int>,
    ): RawTimes {
        lat = latitude
        lng = longitude
        elv = elevationMeters
        timeZone = tzOffsetHours
        jDate = julian(year, month, day) - lng / 360.0

        var times = RawTimes(
            imsak = 5.0, fajr = 5.0, sunrise = 6.0, dhuhr = 12.0,
            asr = 13.0, sunset = 18.0, maghrib = 18.0, isha = 18.0,
        )

        times = computePrayerTimes(times, params, asrParam)
        times = adjustTimes(times, params, highLats)
        times.midnight = if (params.midNightJafari) {
            times.sunset + timeDiff(times.sunset, times.fajr + 24) / 2
        } else {
            times.sunset + timeDiff(times.sunset, times.sunrise + 24) / 2
        }
        return tuneTimes(times, offsetsMinutes)
    }

    // ----------------------- Julian date -----------------------
    private fun julian(yearIn: Int, monthIn: Int, day: Int): Double {
        var year = yearIn
        var month = monthIn
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) +
            floor(30.6001 * (month + 1)) + day + b - 1524.5
    }

    // ----------------------- Core computation -----------------------
    private fun computePrayerTimes(timesIn: RawTimes, params: Params, asrParam: String): RawTimes {
        var times = timesIn / 24.0 // dayPortion

        val imsak = sunAngleTime(params.imsakValue, times.imsak, cw = false)
        val fajr = sunAngleTime(params.fajrAngle, times.fajr, cw = false)
        val sunrise = sunAngleTime(riseSetAngle(), times.sunrise, cw = false)
        val dhuhr = midDay(times.dhuhr)
        val asr = asrTime(asrFactor(asrParam), times.asr)
        val sunset = sunAngleTime(riseSetAngle(), times.sunset, cw = true)
        val maghrib = sunAngleTime(params.maghribValue, times.maghrib, cw = true)
        val isha = sunAngleTime(params.ishaValue, times.isha, cw = true)

        return RawTimes(
            imsak = imsak, fajr = fajr, sunrise = sunrise, dhuhr = dhuhr,
            asr = asr, sunset = sunset, maghrib = maghrib, isha = isha,
        )
    }

    private fun adjustTimes(times: RawTimes, params: Params, highLats: HighLats): RawTimes {
        val shift = timeZone - lng / 15.0
        var t = times + shift
        if (highLats != HighLats.NONE) {
            t = adjustHighLats(t, params, highLats)
        }
        if (params.imsakIsMinutes) {
            t.imsak = t.fajr - params.imsakValue / 60.0
        }
        if (params.maghribIsMinutes) {
            t.maghrib = t.sunset + params.maghribValue / 60.0
        }
        if (params.ishaIsMinutes) {
            t.isha = t.maghrib + params.ishaValue / 60.0
        }
        t.dhuhr += 0.0 // base hour; per-method dhuhr offset is zero
        return t
    }

    private fun adjustHighLats(timesIn: RawTimes, params: Params, highLats: HighLats): RawTimes {
        val times = timesIn.copy()
        val nightTime = timeDiff(times.sunset, times.sunrise)
        times.imsak = adjustHLTime(times.imsak, times.sunrise, params.imsakValue, nightTime, highLats, cw = false)
        times.fajr = adjustHLTime(times.fajr, times.sunrise, params.fajrAngle, nightTime, highLats, cw = false)
        times.isha = adjustHLTime(times.isha, times.sunset, params.ishaValue, nightTime, highLats, cw = true)
        times.maghrib = adjustHLTime(times.maghrib, times.sunset, params.maghribValue, nightTime, highLats, cw = true)
        return times
    }

    private fun adjustHLTime(time: Double, base: Double, angle: Double, night: Double, highLats: HighLats, cw: Boolean): Double {
        val portion = nightPortion(angle, night, highLats)
        val diff = if (!cw) timeDiff(time, base) else timeDiff(base, time)
        return if (time.isNaN() || diff > portion) {
            base + (if (!cw) -portion else portion)
        } else {
            time
        }
    }

    private fun nightPortion(angle: Double, night: Double, highLats: HighLats): Double {
        val portion = when (highLats) {
            HighLats.ANGLE_BASED -> angle / 60.0
            HighLats.ONE_SEVENTH -> 1.0 / 7.0
            else -> 1.0 / 2.0
        }
        return portion * night
    }

    private fun midDay(time: Double): Double {
        val eqt = sunPosition(jDate + time).second
        return fixHour(12 - eqt)
    }

    private fun sunAngleTime(angle: Double, time: Double, cw: Boolean): Double {
        val decl = sunPosition(jDate + time).first
        val noon = midDay(time)
        val numerator = -sin(angle) - sin(decl) * sin(lat)
        val denominator = cos(decl) * cos(lat)
        val t = 1.0 / 15.0 * arccos(numerator / denominator)
        return noon + (if (cw) t else -t)
    }

    private fun asrTime(factor: Int, time: Double): Double {
        val decl = sunPosition(jDate + time).first
        val angle = -arccot(factor + tan(abs(lat - decl)))
        return sunAngleTime(angle, time, cw = true)
    }

    private fun asrFactor(asrParam: String): Int = if (asrParam == "HANAFI") 2 else 1

    private fun riseSetAngle(): Double = 0.833 + 0.0347 * sqrt(elv)

    private fun timeDiff(t1: Double, t2: Double): Double = fixHour(t2 - t1)

    private fun tuneTimes(times: RawTimes, offsetsMinutes: Map<String, Int>): RawTimes {
        return RawTimes(
            imsak = times.imsak + (offsetsMinutes["imsak"] ?: 0) / 60.0,
            fajr = times.fajr + (offsetsMinutes["fajr"] ?: 0) / 60.0,
            sunrise = times.sunrise + (offsetsMinutes["sunrise"] ?: 0) / 60.0,
            dhuhr = times.dhuhr + (offsetsMinutes["dhuhr"] ?: 0) / 60.0,
            asr = times.asr + (offsetsMinutes["asr"] ?: 0) / 60.0,
            sunset = times.sunset + (offsetsMinutes["sunset"] ?: 0) / 60.0,
            maghrib = times.maghrib + (offsetsMinutes["maghrib"] ?: 0) / 60.0,
            isha = times.isha + (offsetsMinutes["isha"] ?: 0) / 60.0,
            midnight = times.midnight + (offsetsMinutes["midnight"] ?: 0) / 60.0,
        )
    }

    /**
     * Returns solar declination and equation of time.
     * Ref: http://aa.usno.navy.mil/faq/docs/SunApprox.php
     */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(g) + 0.020 * sin(2 * g))

        val e = 23.439 - 0.00000036 * d

        val ra = arctan2(cos(e) * sin(l), cos(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = arcsin(sin(e) * sin(l))

        return decl to eqt
    }

    // ----------------------- Operators on RawTimes -----------------------
    private operator fun RawTimes.div(d: Double): RawTimes = copy(
        imsak = imsak / d,
        fajr = fajr / d,
        sunrise = sunrise / d,
        dhuhr = dhuhr / d,
        asr = asr / d,
        sunset = sunset / d,
        maghrib = maghrib / d,
        isha = isha / d,
        midnight = midnight / d,
    )

    private operator fun RawTimes.plus(shift: Double): RawTimes = copy(
        imsak = imsak + shift,
        fajr = fajr + shift,
        sunrise = sunrise + shift,
        dhuhr = dhuhr + shift,
        asr = asr + shift,
        sunset = sunset + shift,
        maghrib = maghrib + shift,
        isha = isha + shift,
        midnight = midnight + shift,
    )
}