package com.praytracker.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(private val context: Context) : PrayerSettings {

    private val prefs: SharedPreferences = context.getSharedPreferences("prayer_times_prefs", Context.MODE_PRIVATE)

    // Using a Simple Flow for reactively listening to setting changes if needed
    private val _settingsChanged = MutableStateFlow(System.currentTimeMillis())
    val settingsChanged: StateFlow<Long> = _settingsChanged

    private fun notifyChanged() {
        _settingsChanged.value = System.currentTimeMillis()
    }

    // --- PRAYER TIMES SETTINGS ---
    // Calculation methods (BatoulApps methods: 0 = JAFARI, 1 = KARACHI, 2 = ISNA, 3 = MWL, 4 = EGYPT, 5 = MAKKAH, 7 = TEHRAN, 8 = GULF, etc.)
    override var calculationMethod: Int
        get() = prefs.getInt("calc_method", 3) // Default: 3 = MWL (Muslim World League)
        set(value) {
            prefs.edit().putInt("calc_method", value).apply()
            notifyChanged()
        }

    override var madhab: Int
        get() = prefs.getInt("madhab", 0) // Default: 0 = SHAFI (Shafi'i, Maliki, Hanbali), 1 = HANAFI
        set(value) {
            prefs.edit().putInt("madhab", value).apply()
            notifyChanged()
        }

    override var highLatitudeRule: Int
        get() = prefs.getInt("high_lat_rule", 0) // Default: 0 = NONE, 1 = MIDNIGHT, 2 = SEVENTH, 3 = TWILIGHT
        set(value) {
            prefs.edit().putInt("high_lat_rule", value).apply()
            notifyChanged()
        }

    // Manual Adjustments (Offsets in minutes)
    override var adjustmentFajr: Int
        get() = prefs.getInt("adj_fajr", 0)
        set(value) {
            prefs.edit().putInt("adj_fajr", value).apply()
            notifyChanged()
        }

    override var adjustmentDhuhr: Int
        get() = prefs.getInt("adj_dhuhr", 0)
        set(value) {
            prefs.edit().putInt("adj_dhuhr", value).apply()
            notifyChanged()
        }

    override var adjustmentAsr: Int
        get() = prefs.getInt("adj_asr", 0)
        set(value) {
            prefs.edit().putInt("adj_asr", value).apply()
            notifyChanged()
        }

    override var adjustmentMaghrib: Int
        get() = prefs.getInt("adj_maghrib", 0)
        set(value) {
            prefs.edit().putInt("adj_maghrib", value).apply()
            notifyChanged()
        }

    override var adjustmentIsha: Int
        get() = prefs.getInt("adj_isha", 0)
        set(value) {
            prefs.edit().putInt("adj_isha", value).apply()
            notifyChanged()
        }

    fun resetPrayerAdjustments() {
        prefs.edit()
            .putInt("adj_fajr", 0)
            .putInt("adj_dhuhr", 0)
            .putInt("adj_asr", 0)
            .putInt("adj_maghrib", 0)
            .putInt("adj_isha", 0)
            .apply()
        notifyChanged()
    }

    // --- LOCATION SETTINGS ---
    var isAutomaticLocation: Boolean
        get() = prefs.getBoolean("auto_location", true)
        set(value) {
            prefs.edit().putBoolean("auto_location", value).apply()
            notifyChanged()
        }

    var latitude: Double
        get() = prefs.getFloat("latitude", 0.0f).toDouble()
        set(value) {
            prefs.edit().putFloat("latitude", value.toFloat()).apply()
            notifyChanged()
        }

    var longitude: Double
        get() = prefs.getFloat("longitude", 0.0f).toDouble()
        set(value) {
            prefs.edit().putFloat("longitude", value.toFloat()).apply()
            notifyChanged()
        }

    override var locationName: String
        get() = prefs.getString("location_name", "Detecting...") ?: "Detecting..."
        set(value) {
            prefs.edit().putString("location_name", value).apply()
            notifyChanged()
        }

    var timezoneId: String
        get() = prefs.getString("timezone_id", "UTC") ?: "UTC"
        set(value) {
            prefs.edit().putString("timezone_id", value).apply()
            notifyChanged()
        }

    // --- DATE SETTINGS ---
    var useArabicNumerals: Boolean
        get() = prefs.getBoolean("use_arabic_numerals", false)
        set(value) {
            prefs.edit().putBoolean("use_arabic_numerals", value).apply()
            notifyChanged()
        }

    override var hijriAdjustment: Int
        get() = prefs.getInt("hijri_adj", 0) // Default: 0 (-2, -1, 0, +1, +2)
        set(value) {
            prefs.edit().putInt("hijri_adj", value).apply()
            notifyChanged()
        }

    fun resetHijriAdjustment() {
        prefs.edit().putInt("hijri_adj", 0).apply()
        notifyChanged()
    }

    // --- NOTIFICATION SETTINGS ---
    var isMasterNotificationEnabled: Boolean
        get() = prefs.getBoolean("notif_master", true)
        set(value) {
            prefs.edit().putBoolean("notif_master", value).apply()
            notifyChanged()
        }

    var isFajrNotificationEnabled: Boolean
        get() = prefs.getBoolean("notif_fajr", true)
        set(value) {
            prefs.edit().putBoolean("notif_fajr", value).apply()
            notifyChanged()
        }

    var isDhuhrNotificationEnabled: Boolean
        get() = prefs.getBoolean("notif_dhuhr", true)
        set(value) {
            prefs.edit().putBoolean("notif_dhuhr", value).apply()
            notifyChanged()
        }

    var isAsrNotificationEnabled: Boolean
        get() = prefs.getBoolean("notif_asr", true)
        set(value) {
            prefs.edit().putBoolean("notif_asr", value).apply()
            notifyChanged()
        }

    var isMaghribNotificationEnabled: Boolean
        get() = prefs.getBoolean("notif_maghrib", true)
        set(value) {
            prefs.edit().putBoolean("notif_maghrib", value).apply()
            notifyChanged()
        }

    var isIshaNotificationEnabled: Boolean
        get() = prefs.getBoolean("notif_isha", true)
        set(value) {
            prefs.edit().putBoolean("notif_isha", value).apply()
            notifyChanged()
        }

    var reminderDelayMinutes: Int
        get() = prefs.getInt("reminder_delay", 15) // Default 15 minutes before / after
        set(value) {
            prefs.edit().putInt("reminder_delay", value).apply()
            notifyChanged()
        }

    var isCustomSoundEnabled: Boolean
        get() = prefs.getBoolean("custom_sound", false)
        set(value) {
            prefs.edit().putBoolean("custom_sound", value).apply()
            notifyChanged()
        }

    // --- TASBIH SETTINGS ---
    var selectedTasbihId: Int
        get() = prefs.getInt("selected_tasbih_id", 1) // default preloaded ID is 1
        set(value) {
            prefs.edit().putInt("selected_tasbih_id", value).apply()
            notifyChanged()
        }

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) {
            prefs.edit().putBoolean("haptic_feedback", value).apply()
            notifyChanged()
        }

    var showTasbihTranslation: Boolean
        get() = prefs.getBoolean("show_tasbih_translation", true)
        set(value) {
            prefs.edit().putBoolean("show_tasbih_translation", value).apply()
            notifyChanged()
        }

    // --- APPEARANCE ---
    var appTheme: String
        get() = prefs.getString("app_theme", "system") ?: "system" // "light", "dark", "system"
        set(value) {
            prefs.edit().putString("app_theme", value).apply()
            notifyChanged()
        }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) {
            prefs.edit().putBoolean("is_first_launch", value).apply()
            notifyChanged()
        }
}
