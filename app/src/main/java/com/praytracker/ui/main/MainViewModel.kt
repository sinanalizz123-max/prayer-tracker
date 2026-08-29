package com.praytracker.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.praytracker.data.settings.Settings
import com.praytracker.data.settings.SettingsRepository
import com.praytracker.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(container: AppContainer) : ViewModel() {

    private val settingsRepository = container.settingsRepository

    val settings: StateFlow<Settings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    var unlockedSession by mutableStateOf(false)
        private set

    fun markSessionUnlocked() {
        unlockedSession = true
    }

    fun setOnboardingDone() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(onboardingDone = true) }
        }
    }
}