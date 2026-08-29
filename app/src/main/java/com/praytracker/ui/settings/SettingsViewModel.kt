package com.praytracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praytracker.data.settings.LocationMode
import com.praytracker.data.settings.Settings
import com.praytracker.data.settings.SettingsRepository
import com.praytracker.data.settings.Theme
import com.praytracker.di.AppContainer
import com.praytracker.prayer.Prayer
import com.praytracker.util.Hash
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settingsRepository = container.settingsRepository

    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            val changed = settingsRepository.snapshot().let(transform)
            val before = settingsRepository.snapshot()
            settingsRepository.update(transform)
            if (calcRelevantChanged(before, changed)) {
                container.prayerAlarmScheduler.rescheduleAll()
            }
        }
    }

    suspend fun refreshLocation() {
        val settings = settingsRepository.snapshot()
        if (settings.locationMode != LocationMode.AUTO) return
        settingsRepository.update { it.copy(locationAutoFailed = false) }
        val location = container.locationProvider.requestLocation()
        if (location != null) {
            val label = container.locationProvider.reverseGeocode(location) ?: "Detected location"
            settingsRepository.update {
                it.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationLabel = label,
                    locationAutoFailed = false,
                    lastLocationTimeMs = System.currentTimeMillis(),
                )
            }
            container.prayerAlarmScheduler.rescheduleAll()
        } else {
            settingsRepository.update { it.copy(locationAutoFailed = true) }
        }
    }

    fun setPasscode(passcode: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(appLockPasscodeHash = Hash.sha256(passcode), appLockEnabled = true) }
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(appLockEnabled = enabled) }
        }
    }

    private fun calcRelevantChanged(before: Settings, after: Settings): Boolean =
        before.calcMethod != after.calcMethod ||
            before.madhab != after.madhab ||
            before.highLatRule != after.highLatRule ||
            before.latitude != after.latitude ||
            before.longitude != after.longitude ||
            before.fajrAdjustment != after.fajrAdjustment ||
            before.dhuhrAdjustment != after.dhuhrAdjustment ||
            before.asrAdjustment != after.asrAdjustment ||
            before.maghribAdjustment != after.maghribAdjustment ||
            before.ishaAdjustment != after.ishaAdjustment ||
            before.notificationsEnabled != after.notificationsEnabled ||
            before.reminderEnabled != after.reminderEnabled ||
            before.reminderMinutes != after.reminderMinutes ||
            Prayer.ORDER.any { before.notifEnabledFor(it) != after.notifEnabledFor(it) } ||
            before.notifSoundEnabled != after.notifSoundEnabled ||
            before.notifVibrateEnabled != after.notifVibrateEnabled
}