package com.praytracker

import android.app.Application
import com.praytracker.data.AppDatabase
import com.praytracker.data.AppRepository
import com.praytracker.data.SettingsManager
import com.praytracker.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val settingsManager by lazy { SettingsManager(this) }
    val repository by lazy { AppRepository(database.tasbihDao(), settingsManager) }

    override fun onCreate() {
        super.onCreate()
        try {
            AlarmScheduler.rescheduleAlarms(this, settingsManager)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
