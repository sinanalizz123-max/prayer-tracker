package com.praytracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun snapshot(): Settings = settings.first()

    suspend fun update(transform: (Settings) -> Settings) {
        val updated = transform(snapshot())
        context.dataStore.edit { prefs -> updated.writeTo(prefs) }
    }

    private fun Settings.writeTo(prefs: Preferences.MutablePreferences) {
        prefs[Keys.ONBOARDING_DONE] = onboardingDone
        prefs[Keys.THEME] = theme.storage
        prefs[Keys.LOCATION_MODE] = locationMode.storage
        prefs[Keys.LATITUDE] = latitude
        prefs[Keys.LONGITUDE] = longitude
        prefs[Keys.LOCATION_LABEL] = locationLabel
        prefs[Keys.LOCATION_AUTO_FAILED] = locationAutoFailed
        prefs[Keys.CALC_METHOD] = calcMethod
        prefs[Keys.MADHAB] = madhab
        prefs[Keys.HIGH_LAT_RULE] = highLatRule
        prefs[Keys.FAJR_ADJ] = fajrAdjustment
        prefs[Keys.DHUHR_ADJ] = dhuhrAdjustment
        prefs[Keys.ASR_ADJ] = asrAdjustment
        prefs[Keys.MAGHRIB_ADJ] = maghribAdjustment
        prefs[Keys.ISHA_ADJ] = ishaAdjustment
        prefs[Keys.HIJRI_OFFSET] = hijriOffsetDays
        prefs[Keys.NOTIFICATIONS_ENABLED] = notificationsEnabled
        prefs[Keys.FAJR_NOTIF] = fajrNotifEnabled
        prefs[Keys.DHUHR_NOTIF] = dhuhrNotifEnabled
        prefs[Keys.ASR_NOTIF] = asrNotifEnabled
        prefs[Keys.MAGHRIB_NOTIF] = maghribNotifEnabled
        prefs[Keys.ISHA_NOTIF] = ishaNotifEnabled
        prefs[Keys.REMINDER_ENABLED] = reminderEnabled
        prefs[Keys.REMINDER_MINUTES] = reminderMinutes
        prefs[Keys.NOTIF_SOUND] = notifSoundEnabled
        prefs[Keys.NOTIF_VIBRATE] = notifVibrateEnabled
        prefs[Keys.TASBIH_HAPTICS] = tasbihHapticsEnabled
        prefs[Keys.TASBIH_AUTO_SWITCH] = tasbihAutoSwitchToNext
        prefs[Keys.APP_LOCK_ENABLED] = appLockEnabled
        prefs[Keys.APP_LOCK_PASSCODE_HASH] = appLockPasscodeHash
        prefs[Keys.LAST_LOCATION_MS] = lastLocationTimeMs
    }

    private fun Preferences.toSettings(): Settings {
        fun bool(key: Preferences.Key<Boolean>, def: Boolean) = this[key] ?: def
        return Settings(
            onboardingDone = bool(Keys.ONBOARDING_DONE, false),
            theme = Theme.fromStorage(this[Keys.THEME]),
            locationMode = LocationMode.fromStorage(this[Keys.LOCATION_MODE]),
            latitude = this[Keys.LATITUDE] ?: 21.4225,
            longitude = this[Keys.LONGITUDE] ?: 39.8262,
            locationLabel = this[Keys.LOCATION_LABEL] ?: "Makkah, Saudi Arabia",
            locationAutoFailed = bool(Keys.LOCATION_AUTO_FAILED, false),
            calcMethod = this[Keys.CALC_METHOD] ?: "MWL",
            madhab = this[Keys.MADHAB] ?: "SHAFI",
            highLatRule = this[Keys.HIGH_LAT_RULE] ?: "NIGHT_MIDDLE",
            fajrAdjustment = this[Keys.FAJR_ADJ] ?: 0,
            dhuhrAdjustment = this[Keys.DHUHR_ADJ] ?: 0,
            asrAdjustment = this[Keys.ASR_ADJ] ?: 0,
            maghribAdjustment = this[Keys.MAGHRIB_ADJ] ?: 0,
            ishaAdjustment = this[Keys.ISHA_ADJ] ?: 0,
            hijriOffsetDays = this[Keys.HIJRI_OFFSET] ?: 0,
            notificationsEnabled = bool(Keys.NOTIFICATIONS_ENABLED, true),
            fajrNotifEnabled = bool(Keys.FAJR_NOTIF, true),
            dhuhrNotifEnabled = bool(Keys.DHUHR_NOTIF, true),
            asrNotifEnabled = bool(Keys.ASR_NOTIF, true),
            maghribNotifEnabled = bool(Keys.MAGHRIB_NOTIF, true),
            ishaNotifEnabled = bool(Keys.ISHA_NOTIF, true),
            reminderEnabled = bool(Keys.REMINDER_ENABLED, false),
            reminderMinutes = this[Keys.REMINDER_MINUTES] ?: 5,
            notifSoundEnabled = bool(Keys.NOTIF_SOUND, true),
            notifVibrateEnabled = bool(Keys.NOTIF_VIBRATE, true),
            tasbihHapticsEnabled = bool(Keys.TASBIH_HAPTICS, true),
            tasbihAutoSwitchToNext = bool(Keys.TASBIH_AUTO_SWITCH, false),
            appLockEnabled = bool(Keys.APP_LOCK_ENABLED, false),
            appLockPasscodeHash = this[Keys.APP_LOCK_PASSCODE_HASH],
            lastLocationTimeMs = this[Keys.LAST_LOCATION_MS] ?: 0L,
        )
    }

    object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME = stringPreferencesKey("theme")
        val LOCATION_MODE = stringPreferencesKey("location_mode")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val LOCATION_LABEL = stringPreferencesKey("location_label")
        val LOCATION_AUTO_FAILED = booleanPreferencesKey("location_auto_failed")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val MADHAB = stringPreferencesKey("madhab")
        val HIGH_LAT_RULE = stringPreferencesKey("high_lat_rule")
        val FAJR_ADJ = intPreferencesKey("adj_fajr")
        val DHUHR_ADJ = intPreferencesKey("adj_dhuhr")
        val ASR_ADJ = intPreferencesKey("adj_asr")
        val MAGHRIB_ADJ = intPreferencesKey("adj_maghrib")
        val ISHA_ADJ = intPreferencesKey("adj_isha")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val FAJR_NOTIF = booleanPreferencesKey("fajr_notif")
        val DHUHR_NOTIF = booleanPreferencesKey("dhuhr_notif")
        val ASR_NOTIF = booleanPreferencesKey("asr_notif")
        val MAGHRIB_NOTIF = booleanPreferencesKey("maghrib_notif")
        val ISHA_NOTIF = booleanPreferencesKey("isha_notif")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val NOTIF_SOUND = booleanPreferencesKey("notif_sound")
        val NOTIF_VIBRATE = booleanPreferencesKey("notif_vibrate")
        val TASBIH_HAPTICS = booleanPreferencesKey("tasbih_haptics")
        val TASBIH_AUTO_SWITCH = booleanPreferencesKey("tasbih_auto_switch")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_PASSCODE_HASH = stringPreferencesKey("app_lock_passcode_hash")
        val LAST_LOCATION_MS = longPreferencesKey("last_location_ms")
    }
}