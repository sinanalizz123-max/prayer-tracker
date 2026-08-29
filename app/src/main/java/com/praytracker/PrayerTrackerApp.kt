package com.praytracker

import android.app.Application
import com.praytracker.di.AppContainer
import kotlinx.coroutines.launch

class PrayerTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            container.tasbihRepository.seedDefaultsIfEmpty()
            container.prayerAlarmScheduler.rescheduleAll()
        }
    }
}