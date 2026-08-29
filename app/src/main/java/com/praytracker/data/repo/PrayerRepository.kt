package com.praytracker.data.repo

import com.praytracker.data.db.PrayerRecord
import com.praytracker.data.db.PrayerRecordDao
import com.praytracker.prayer.Prayer
import com.praytracker.prayer.PrayerStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class PrayerRepository(private val dao: PrayerRecordDao) {

    fun observeAll(): Flow<List<PrayerRecord>> = dao.observeAll()

    suspend fun record(date: LocalDate): PrayerRecord = dao.get(iso(date)) ?: PrayerRecord.forDate(date)

    suspend fun setStatus(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        dao.upsert(record(date).withStatus(prayer, status))
    }

    suspend fun recordsBetween(from: LocalDate, to: LocalDate): List<PrayerRecord> =
        dao.getRange(iso(from), iso(to))

    suspend fun import(records: List<PrayerRecord>) {
        records.forEach { dao.upsert(it) }
    }

    suspend fun delete(date: LocalDate) = dao.delete(iso(date))

    suspend fun clearAll() = dao.clearAll()

    private fun iso(date: LocalDate): String = date.toString()
}