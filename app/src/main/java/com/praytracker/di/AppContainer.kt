package com.praytracker.di

import android.content.Context
import androidx.room.Room
import com.praytracker.backup.BackupManager
import com.praytracker.data.db.AppDatabase
import com.praytracker.data.repo.PrayerRepository
import com.praytracker.data.repo.PrayerTimesRepository
import com.praytracker.data.repo.TasbihRepository
import com.praytracker.data.settings.SettingsRepository
import com.praytracker.loc.LocationProvider
import com.praytracker.notif.PrayerAlarmScheduler
import com.praytracker.notif.PrayerNotifier

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "praytracker.db").build()
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }
    val locationProvider: LocationProvider by lazy { LocationProvider(context) }

    val prayerRepository: PrayerRepository by lazy { PrayerRepository(database.prayerRecordDao()) }
    val tasbihRepository: TasbihRepository by lazy { TasbihRepository(database.tasbihDao()) }
    val prayerTimesRepository: PrayerTimesRepository by lazy { PrayerTimesRepository(settingsRepository) }

    val prayerNotifier: PrayerNotifier by lazy { PrayerNotifier(context) }
    val prayerAlarmScheduler: PrayerAlarmScheduler by lazy {
        PrayerAlarmScheduler(context, prayerTimesRepository, settingsRepository)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(context, prayerRepository, tasbihRepository, settingsRepository)
    }

    val scope: kotlinx.coroutines.CoroutineScope by lazy {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    }
}