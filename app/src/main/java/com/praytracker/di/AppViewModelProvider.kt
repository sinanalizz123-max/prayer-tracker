package com.praytracker.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.praytracker.PrayerTrackerApp
import com.praytracker.ui.history.HistoryViewModel
import com.praytracker.ui.main.MainViewModel
import com.praytracker.ui.settings.SettingsViewModel
import com.praytracker.ui.tasbih.TasbihViewModel
import com.praytracker.ui.today.TodayViewModel

object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { MainViewModel(app().container) }
        initializer { TodayViewModel(app().container) }
        initializer { HistoryViewModel(app().container) }
        initializer { TasbihViewModel(app().container) }
        initializer { SettingsViewModel(app().container) }
    }

    private fun CreationExtras.app(): PrayerTrackerApp =
        this[APPLICATION_KEY] as PrayerTrackerApp
}