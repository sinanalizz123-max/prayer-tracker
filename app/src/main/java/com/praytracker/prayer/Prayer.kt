package com.praytracker.prayer

enum class Prayer(val displayName: String) {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha"),
    ;

    companion object {
        val ORDER = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

enum class PrayerStatus(val label: String, val symbol: String) {
    NOT_RECORDED("Not recorded", "–"),
    PRAYED("Prayed", "✓"),
    DELAYED("Delayed", "~"),
    NOT_DID("Not Did", "×");

    companion object {
        fun fromStored(value: String?): PrayerStatus =
            entries.firstOrNull { it.name == value } ?: NOT_RECORDED
    }
}