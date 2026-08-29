package com.praytracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PrayerRecord::class,
        TasbihEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerRecordDao(): PrayerRecordDao
    abstract fun tasbihDao(): TasbihDao
}