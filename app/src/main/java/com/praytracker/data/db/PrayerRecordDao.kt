package com.praytracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerRecordDao {

    @Query("SELECT * FROM prayer_records WHERE date = :date")
    suspend fun get(date: String): PrayerRecord?

    @Query("SELECT * FROM prayer_records WHERE date BETWEEN :from AND :to")
    suspend fun getRange(from: String, to: String): List<PrayerRecord>

    @Query("SELECT * FROM prayer_records ORDER BY date DESC")
    fun observeAll(): Flow<List<PrayerRecord>>

    @Query("SELECT * FROM prayer_records")
    suspend fun getAll(): List<PrayerRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PrayerRecord)

    @Query("DELETE FROM prayer_records WHERE date = :date")
    suspend fun delete(date: String)

    @Query("DELETE FROM prayer_records")
    suspend fun clearAll()
}