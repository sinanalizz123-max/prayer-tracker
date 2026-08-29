package com.praytracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.praytracker.prayer.PrayerStatus
import java.time.LocalDate

/**
 * A day's prayer statuses, keyed by ISO local date (yyyy-MM-dd).
 * Historically independent from current calculation settings.
 */
@Entity(tableName = "prayer_records")
data class PrayerRecord(
    @PrimaryKey val date: String,
    val fajrStatus: String = PrayerStatus.NOT_RECORDED.name,
    val dhuhrStatus: String = PrayerStatus.NOT_RECORDED.name,
    val asrStatus: String = PrayerStatus.NOT_RECORDED.name,
    val maghribStatus: String = PrayerStatus.NOT_RECORDED.name,
    val ishaStatus: String = PrayerStatus.NOT_RECORDED.name,
) {
    fun status(prayer: com.praytracker.prayer.Prayer): PrayerStatus = PrayerStatus.fromStored(
        when (prayer) {
            com.praytracker.prayer.Prayer.FAJR -> fajrStatus
            com.praytracker.prayer.Prayer.DHUHR -> dhuhrStatus
            com.praytracker.prayer.Prayer.ASR -> asrStatus
            com.praytracker.prayer.Prayer.MAGHRIB -> maghribStatus
            com.praytracker.prayer.Prayer.ISHA -> ishaStatus
        }
    )

    fun withStatus(prayer: com.praytracker.prayer.Prayer, status: PrayerStatus): PrayerRecord =
        when (prayer) {
            com.praytracker.prayer.Prayer.FAJR -> copy(fajrStatus = status.name)
            com.praytracker.prayer.Prayer.DHUHR -> copy(dhuhrStatus = status.name)
            com.praytracker.prayer.Prayer.ASR -> copy(asrStatus = status.name)
            com.praytracker.prayer.Prayer.MAGHRIB -> copy(maghribStatus = status.name)
            com.praytracker.prayer.Prayer.ISHA -> copy(ishaStatus = status.name)
        }

    fun prayedCount(): Int = listOf(fajrStatus, dhuhrStatus, asrStatus, maghribStatus, ishaStatus)
        .count { it == PrayerStatus.PRAYED.name }

    companion object {
        fun forDate(date: LocalDate): PrayerRecord = PrayerRecord(date = date.toString())

        fun isoDate(date: LocalDate): String = date.toString()
    }
}