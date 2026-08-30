package com.praytracker.ui

import android.app.Application
import android.content.Context
import android.hardware.GeomagneticField
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.praytracker.MainApplication
import com.praytracker.data.SettingsManager
import com.praytracker.data.TasbihItem
import com.praytracker.util.AlarmScheduler
import com.praytracker.util.CompassManager
import com.praytracker.util.HijriHelper
import com.praytracker.util.PrayerCalculator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    val repository = app.repository
    val settings: SettingsManager = app.settingsManager

    val compassManager = CompassManager(application)

    // --- NAVIGATION STATE ---
    private val _currentTab = MutableStateFlow(0) // 0: Prayer, 1: Tasbih, 2: Qibla, 3: More
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _currentMoreSubPage = MutableStateFlow<String?>(null) // "RAMADAN", "SETTINGS", "ABOUT", null
    val currentMoreSubPage: StateFlow<String?> = _currentMoreSubPage.asStateFlow()

    fun selectTab(tab: Int) {
        _currentTab.value = tab
        if (tab == 2) {
            compassManager.start()
        } else {
            compassManager.stop()
        }
    }

    fun selectMoreSubPage(page: String?) {
        _currentMoreSubPage.value = page
    }

    // --- TICKING CLOCK & REACTIVE PRAYER CALCULATIONS ---
    private val _currentTime = MutableStateFlow(ZonedDateTime.now())
    val currentTime: StateFlow<ZonedDateTime> = _currentTime.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = ZonedDateTime.now()
                delay(1000)
            }
        }
    }

    /**
     * Everything that affects a computed PrayerSchedule. Astronomical recalculation
     * is expensive, so it only runs when this key actually changes (date rollover,
     * location, timezone, or a calculation setting) instead of every second.
     */
    private data class ScheduleKey(
        val date: LocalDate,
        val latitude: Double,
        val longitude: Double,
        val timezoneId: String,
        val calculationMethod: Int,
        val madhab: Int,
        val highLatitudeRule: Int,
        val adjustmentFajr: Int,
        val adjustmentDhuhr: Int,
        val adjustmentAsr: Int,
        val adjustmentMaghrib: Int,
        val adjustmentIsha: Int,
        val hijriAdjustment: Int,
        val locationName: String
    )

    private fun currentScheduleKey(date: LocalDate): ScheduleKey = ScheduleKey(
        date = date,
        latitude = settings.latitude,
        longitude = settings.longitude,
        timezoneId = settings.timezoneId,
        calculationMethod = settings.calculationMethod,
        madhab = settings.madhab,
        highLatitudeRule = settings.highLatitudeRule,
        adjustmentFajr = settings.adjustmentFajr,
        adjustmentDhuhr = settings.adjustmentDhuhr,
        adjustmentAsr = settings.adjustmentAsr,
        adjustmentMaghrib = settings.adjustmentMaghrib,
        adjustmentIsha = settings.adjustmentIsha,
        hijriAdjustment = settings.hijriAdjustment,
        locationName = settings.locationName
    )

    // Only the date changes from the per-second clock; emitted at most once per day.
    private val currentDate: StateFlow<LocalDate> = _currentTime
        .map { it.toLocalDate() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LocalDate.now()
        )

    private val scheduleKey: StateFlow<ScheduleKey> = combine(
        settings.settingsChanged,
        currentDate
    ) { _, date ->
        currentScheduleKey(date)
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = currentScheduleKey(LocalDate.now())
    )

    private fun scheduleFor(key: ScheduleKey, dayOffset: Long): PrayerCalculator.PrayerSchedule {
        val date = key.date.plusDays(dayOffset)
        return PrayerCalculator.calculateSchedule(
            lat = key.latitude,
            lon = key.longitude,
            timezoneId = key.timezoneId,
            localDate = date,
            settings = settings
        )
    }

    val prayerSchedule: StateFlow<PrayerCalculator.PrayerSchedule> = scheduleKey
        .map { scheduleFor(it, 0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = scheduleFor(currentScheduleKey(LocalDate.now()), 0)
        )

    private val tomorrowSchedule: StateFlow<PrayerCalculator.PrayerSchedule> = scheduleKey
        .map { scheduleFor(it, 1) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = scheduleFor(currentScheduleKey(LocalDate.now()), 1)
        )

    /**
     * Recomputed every second, but against the cached schedules: only the
     * countdown arithmetic runs here, never the astronomical calculations.
     */
    val nextPrayerInfo: StateFlow<PrayerCalculator.NextPrayerInfo?> = combine(
        prayerSchedule,
        tomorrowSchedule,
        _currentTime
    ) { today, tomorrow, now ->
        try {
            PrayerCalculator.getNextPrayer(today, tomorrow, now)
        } catch (e: Exception) {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val hijriDate: StateFlow<HijriHelper.HijriDateInfo> = combine(
        settings.settingsChanged,
        currentDate
    ) { _, date ->
        HijriHelper.getHijriDate(date, settings.hijriAdjustment, settings.useArabicNumerals)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HijriHelper.getHijriDate(LocalDate.now(), settings.hijriAdjustment, settings.useArabicNumerals)
    )

    fun setHijriAdjustment(adjustment: Int) {
        settings.hijriAdjustment = adjustment.coerceIn(-3, 3)
        onSettingsChanged()
    }

    val currentActivePrayer: StateFlow<String> = combine(
        prayerSchedule,
        _currentTime
    ) { schedule, time ->
        PrayerCalculator.getCurrentActivePrayer(schedule, time)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Dhuhr"
    )

    // --- GEOLOCATION DETECTION ---
    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    fun detectLocation() {
        _isDetectingLocation.value = true
        _locationError.value = null

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(app)
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val lat = location.latitude
                            val lon = location.longitude
                            val timezoneId = java.util.TimeZone.getDefault().id

                            var cityName = "Delhi, India"
                            try {
                                val geocoder = Geocoder(app, Locale.getDefault())
                                val addresses = geocoder.getFromLocation(lat, lon, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Detected City"
                                    val country = addresses[0].countryName ?: ""
                                    cityName = if (country.isNotBlank() && country != city) "$city, $country" else city
                                }
                            } catch (e: Exception) {
                                // fallback
                            }

                            withContext(Dispatchers.Main) {
                                settings.latitude = lat
                                settings.longitude = lon
                                settings.timezoneId = timezoneId
                                settings.locationName = cityName
                                settings.isAutomaticLocation = true
                                _isDetectingLocation.value = false
                                AlarmScheduler.rescheduleAlarms(app, settings)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                _locationError.value = "Unable to resolve location. You can select town manually."
                                _isDetectingLocation.value = false
                            }
                        }
                    }
                } else {
                    _locationError.value = "Location currently unavailable. You can select town manually."
                    _isDetectingLocation.value = false
                }
            }.addOnFailureListener {
                _locationError.value = "Location detection failed. You can select town manually."
                _isDetectingLocation.value = false
            }
        } catch (e: SecurityException) {
            _locationError.value = "Location permission is required for auto-detection."
            _isDetectingLocation.value = false
        }
    }

    fun setManualLocation(name: String, lat: Double, lon: Double, tzId: String) {
        settings.isAutomaticLocation = false
        settings.locationName = name
        settings.latitude = lat
        settings.longitude = lon
        settings.timezoneId = tzId
        AlarmScheduler.rescheduleAlarms(app, settings)
    }

    fun onSettingsChanged() {
        AlarmScheduler.rescheduleAlarms(app, settings)
    }

    // --- COMPASS & QIBLA ANGLE ---
    val compassHeading: StateFlow<Float> = compassManager.heading
    val compassAccuracy: StateFlow<Int> = compassManager.accuracy

    // The raw compass heading is measured relative to *magnetic* north, but the
    // Qibla bearing is relative to *true* (geographic) north. Correct for the
    // local magnetic declination so the needle points at the real Qibla.
    // Declination drifts much slower than the per-second clock, so it is only
    // recomputed when the date or a setting changes.
    private val magneticDeclination: StateFlow<Float> = combine(
        settings.settingsChanged,
        currentDate
    ) { _, date ->
        magneticDeclinationFor(date.atStartOfDay(_currentTime.value.zone))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = magneticDeclinationFor(ZonedDateTime.now())
    )

    val trueHeading: StateFlow<Float> = combine(
        compassManager.heading,
        magneticDeclination
    ) { heading, declination ->
        val trueBearing = heading + declination
        ((trueBearing % 360f) + 360f) % 360f
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    private fun magneticDeclinationFor(now: ZonedDateTime): Float {
        return try {
            val field = GeomagneticField(
                settings.latitude.toFloat(),
                settings.longitude.toFloat(),
                0f,
                now.toInstant().toEpochMilli()
            )
            field.declination
        } catch (e: Exception) {
            0f
        }
    }

    private fun computeQiblaDirection(): Float {
        return try {
            val coordinates = com.batoulapps.adhan.Coordinates(settings.latitude, settings.longitude)
            com.batoulapps.adhan.Qibla(coordinates).direction.toFloat()
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * True when a real location has been set. The default coordinates (0.0, 0.0)
     * and "Detecting..." placeholder mean the Qibla bearing would be meaningless.
     */
    val locationResolved: Boolean
        get() = !(settings.latitude == 0.0 && settings.longitude == 0.0) &&
                settings.locationName != "Detecting..."

    val qiblaDirection: StateFlow<Float> = settings.settingsChanged
        .map { computeQiblaDirection() }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = computeQiblaDirection()
        )

    // --- TASBIH LIBRARY & COUNTING ---
    val tasbihList: StateFlow<List<TasbihItem>> = repository.allTasbihItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun incrementTasbih(item: TasbihItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCount = item.currentCount + 1
            repository.updateTasbih(item.copy(currentCount = newCount))
            
            if (settings.isHapticFeedbackEnabled) {
                triggerHapticFeedback()
            }
        }
    }

    fun resetTasbih(item: TasbihItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTasbih(item.copy(currentCount = 0))
        }
    }

    fun addCustomTasbih(arabic: String, english: String, target: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = TasbihItem(
                arabicText = arabic,
                englishText = english,
                targetCount = target,
                isCustom = true
            )
            repository.insertTasbih(item)
        }
    }

    fun editTasbih(item: TasbihItem, arabic: String, english: String, target: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTasbih(item.copy(
                arabicText = arabic,
                englishText = english,
                targetCount = target
            ))
        }
    }

    fun deleteTasbih(item: TasbihItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.isCustom) {
                if (settings.selectedTasbihId == item.id) {
                    settings.selectedTasbihId = 1
                }
                repository.deleteTasbih(item)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // ignore vibration failures
        }
    }

    override fun onCleared() {
        super.onCleared()
        compassManager.stop()
    }
}
