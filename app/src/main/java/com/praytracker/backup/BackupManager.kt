package com.praytracker.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.praytracker.data.db.PrayerRecord
import com.praytracker.data.db.TasbihEntity
import com.praytracker.data.repo.PrayerRepository
import com.praytracker.data.repo.TasbihRepository
import com.praytracker.data.settings.SettingsRepository
import com.praytracker.prayer.PrayerStatus
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val prayerRepository: PrayerRepository,
    private val tasbihRepository: TasbihRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val statusName = PrayerStatus.entries.associateBy { it.name }

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "praytracker")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", LocalDate.now().toString())

        val records = JSONArray()
        prayerRepository.observeAll().first().forEach { record ->
            records.put(
                JSONObject().apply {
                    put("date", record.date)
                    put("fajr", record.fajrStatus)
                    put("dhuhr", record.dhuhrStatus)
                    put("asr", record.asrStatus)
                    put("maghrib", record.maghribStatus)
                    put("isha", record.ishaStatus)
                }
            )
        }
        root.put("prayerRecords", records)

        val tasbih = JSONArray()
        tasbihRepository.getAll().forEach { t ->
            tasbih.put(
                JSONObject().apply {
                    put("arabic", t.arabic)
                    put("translation", t.translation)
                    put("target", t.target)
                    put("count", t.count)
                    put("orderIndex", t.orderIndex)
                }
            )
        }
        root.put("tasbih", tasbih)

        val settings = settingsRepository.snapshot()
        root.put("settings", JSONObject().apply {
            put("locationMode", settings.locationMode.storage)
            put("latitude", settings.latitude)
            put("longitude", settings.longitude)
            put("locationLabel", settings.locationLabel)
            put("calcMethod", settings.calcMethod)
            put("madhab", settings.madhab)
            put("highLatRule", settings.highLatRule)
            put("fajrAdj", settings.fajrAdjustment)
            put("dhuhrAdj", settings.dhuhrAdjustment)
            put("asrAdj", settings.asrAdjustment)
            put("maghribAdj", settings.maghribAdjustment)
            put("ishaAdj", settings.ishaAdjustment)
            put("hijriOffset", settings.hijriOffsetDays)
            put("notificationsEnabled", settings.notificationsEnabled)
            put("reminderEnabled", settings.reminderEnabled)
            put("reminderMinutes", settings.reminderMinutes)
        })

        return root.toString(2)
    }

    suspend fun importJson(json: String): ImportResult {
        return try {
            val root = JSONObject(json)
            if (root.optString("app") != "praytracker") {
                return ImportResult(false, "Not a praytracker backup file")
            }
            val version = root.optInt("version", 1)
            if (version > BACKUP_VERSION) {
                return ImportResult(false, "Backup was made by a newer version of the app")
            }

            val records = root.optJSONArray("prayerRecords")
            if (records != null) {
                prayerRepository.clearAll()
                val toImport = mutableListOf<PrayerRecord>()
                for (i in 0 until records.length()) {
                    val r = records.getJSONObject(i)
                    toImport.add(
                        PrayerRecord(
                            date = r.getString("date"),
                            fajrStatus = validStatus(r.optString("fajr")),
                            dhuhrStatus = validStatus(r.optString("dhuhr")),
                            asrStatus = validStatus(r.optString("asr")),
                            maghribStatus = validStatus(r.optString("maghrib")),
                            ishaStatus = validStatus(r.optString("isha")),
                        )
                    )
                }
                prayerRepository.import(toImport)
            }

            val tasbih = root.optJSONArray("tasbih")
            if (tasbih != null) {
                tasbihRepository.clearAll()
                for (i in 0 until tasbih.length()) {
                    val t = tasbih.getJSONObject(i)
                    tasbihRepository.upsert(
                        TasbihEntity(
                            id = (i + 1).toLong(),
                            arabic = t.getString("arabic"),
                            translation = t.optString("translation").ifBlank { null },
                            target = t.optInt("target", 33),
                            count = t.optInt("count", 0).coerceIn(0, t.optInt("target", 33)),
                            orderIndex = t.optInt("orderIndex", i),
                        )
                    )
                }
            }

            val settings = root.optJSONObject("settings")
            if (settings != null) {
                settingsRepository.update { current ->
                    current.copy(
                        locationMode = try {
                            com.praytracker.data.settings.LocationMode.valueOf(settings.optString("locationMode", ""))
                        } catch (_: Exception) {
                            current.locationMode
                        },
                        latitude = settings.optDouble("latitude", current.latitude),
                        longitude = settings.optDouble("longitude", current.longitude),
                        locationLabel = settings.optString("locationLabel", current.locationLabel),
                        calcMethod = settings.optString("calcMethod", current.calcMethod),
                        madhab = settings.optString("madhab", current.madhab),
                        highLatRule = settings.optString("highLatRule", current.highLatRule),
                        fajrAdjustment = settings.optInt("fajrAdj", current.fajrAdjustment),
                        dhuhrAdjustment = settings.optInt("dhuhrAdj", current.dhuhrAdjustment),
                        asrAdjustment = settings.optInt("asrAdj", current.asrAdjustment),
                        maghribAdjustment = settings.optInt("maghribAdj", current.maghribAdjustment),
                        ishaAdjustment = settings.optInt("ishaAdj", current.ishaAdjustment),
                        hijriOffsetDays = settings.optInt("hijriOffset", current.hijriOffsetDays),
                        notificationsEnabled = settings.optBoolean("notificationsEnabled", current.notificationsEnabled),
                        reminderEnabled = settings.optBoolean("reminderEnabled", current.reminderEnabled),
                        reminderMinutes = settings.optInt("reminderMinutes", current.reminderMinutes),
                    )
                }
            }

            ImportResult(true, null)
        } catch (e: Exception) {
            ImportResult(false, "Invalid backup file: ${e.message}")
        }
    }

    fun createShareUri(): Uri {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(dir, "praytracker-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    suspend fun writeExportToFile(): Uri {
        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(dir, "praytracker-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json")
        file.writeText(exportJson())
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun validStatus(value: String): String =
        statusName[value]?.name ?: PrayerStatus.NOT_RECORDED.name

    data class ImportResult(val success: Boolean, val error: String?)

    companion object {
        const val BACKUP_VERSION = 1
    }
}