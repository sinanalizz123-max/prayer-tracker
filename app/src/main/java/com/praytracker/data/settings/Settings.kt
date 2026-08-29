package com.praytracker.data.settings

import com.praytracker.prayer.Prayer

data class Settings(
    val onboardingDone: Boolean = false,
    val theme: Theme = Theme.SYSTEM,

    val locationMode: LocationMode = LocationMode.AUTO,
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val locationLabel: String = "Makkah, Saudi Arabia",
    val locationAutoFailed: Boolean = false,

    val calcMethod: String = "MWL",
    val madhab: String = "SHAFI",
    val highLatRule: String = "NIGHT_MIDDLE",

    val fajrAdjustment: Int = 0,
    val dhuhrAdjustment: Int = 0,
    val asrAdjustment: Int = 0,
    val maghribAdjustment: Int = 0,
    val ishaAdjustment: Int = 0,
    val hijriOffsetDays: Int = 0,

    val notificationsEnabled: Boolean = true,
    val fajrNotifEnabled: Boolean = true,
    val dhuhrNotifEnabled: Boolean = true,
    val asrNotifEnabled: Boolean = true,
    val maghribNotifEnabled: Boolean = true,
    val ishaNotifEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 5,
    val notifSoundEnabled: Boolean = true,
    val notifVibrateEnabled: Boolean = true,

    val tasbihHapticsEnabled: Boolean = true,
    val tasbihAutoSwitchToNext: Boolean = false,

    val appLockEnabled: Boolean = false,
    val appLockPasscodeHash: String? = null,

    val baseAdhanEnabled: Boolean = true, // not used; placeholder for future

    val lastLocationTimeMs: Long = 0L,
) {
    fun adjustmentFor(prayer: Prayer): Int = when (prayer) {
        Prayer.FAJR -> fajrAdjustment
        Prayer.DHUHR -> dhuhrAdjustment
        Prayer.ASR -> asrAdjustment
        Prayer.MAGHRIB -> maghribAdjustment
        Prayer.ISHA -> ishaAdjustment
    }

    fun notifEnabledFor(prayer: Prayer): Boolean = when (prayer) {
        Prayer.FAJR -> fajrNotifEnabled
        Prayer.DHUHR -> dhuhrNotifEnabled
        Prayer.ASR -> asrNotifEnabled
        Prayer.MAGHRIB -> maghribNotifEnabled
        Prayer.ISHA -> ishaNotifEnabled
    }

    val adjustments: Map<Prayer, Int>
        get() = mapOf(
            Prayer.FAJR to fajrAdjustment,
            Prayer.DHUHR to dhuhrAdjustment,
            Prayer.ASR to asrAdjustment,
            Prayer.MAGHRIB to maghribAdjustment,
            Prayer.ISHA to ishaAdjustment,
        )
}

enum class Theme(val storage: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): Theme =
            entries.firstOrNull { it.storage == value } ?: SYSTEM
    }
}

enum class LocationMode(val storage: String) {
    AUTO("AUTO"),
    MANUAL("MANUAL");

    companion object {
        fun fromStorage(value: String?): LocationMode =
            entries.firstOrNull { it.storage == value } ?: AUTO
    }
}