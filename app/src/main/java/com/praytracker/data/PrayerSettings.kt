package com.praytracker.data

/**
 * Read-only projection of the prayer-time related settings consumed by
 * [com.praytracker.util.PrayerCalculator]. Keeps calculation logic free of
 * Android dependencies so it can be unit tested on the JVM.
 */
interface PrayerSettings {
    val calculationMethod: Int
    val madhab: Int
    val highLatitudeRule: Int
    val adjustmentFajr: Int
    val adjustmentDhuhr: Int
    val adjustmentAsr: Int
    val adjustmentMaghrib: Int
    val adjustmentIsha: Int
    val hijriAdjustment: Int
    val locationName: String
}