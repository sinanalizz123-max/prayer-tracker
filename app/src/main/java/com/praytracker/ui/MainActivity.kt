package com.praytracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.praytracker.di.AppViewModelProvider
import com.praytracker.data.settings.Settings
import com.praytracker.ui.lock.LockScreen
import com.praytracker.ui.main.MainViewModel
import com.praytracker.ui.nav.PrayTrackerNavHost
import com.praytracker.ui.onboarding.OnboardingScreen
import com.praytracker.ui.theme.PrayerTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val settings by vm.settings.collectAsStateWithLifecycle()

            val darkTheme = when (settings.theme) {
                com.praytracker.data.settings.Theme.DARK -> true
                com.praytracker.data.settings.Theme.LIGHT -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            PrayerTrackerTheme(darkTheme = darkTheme) {
                when {
                    !settings.onboardingDone -> {
                        OnboardingScreen(
                            onStart = { vm.setOnboardingDone() },
                        )
                    }

                    settings.appLockEnabled && !vm.unlockedSession -> {
                        LockScreen(
                            settings = settings,
                            onUnlocked = { vm.markSessionUnlocked() },
                        )
                    }

                    else -> {
                        PrayTrackerNavHost()
                    }
                }
            }
        }
    }
}