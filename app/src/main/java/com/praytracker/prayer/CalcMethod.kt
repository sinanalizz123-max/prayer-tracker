package com.praytracker.prayer

/**
 * Supported prayer-time calculation methods.
 * Angles/parameters follow the well-established PrayTimes.org definitions.
 */
enum class CalcMethod(
    val displayName: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutes: Int?,
    val maghribAngle: Double? = null,
    val maghribMinutes: Int? = null,
    val jafariMidnight: Boolean = false,
) {
    MWL(
        "Muslim World League", 18.0, 17.0, null,
    ),
    ISNA(
        "Islamic Society of North America (ISNA)", 15.0, 15.0, null,
    ),
    EGYPT(
        "Egyptian General Authority of Survey", 19.5, 17.5, null,
    ),
    MAKKAH(
        "Umm al-Qura, Makkah", 18.5, null, 90,
    ),
    KARACHI(
        "University of Islamic Sciences, Karachi", 18.0, 18.0, null,
    ),
    TEHRAN(
        "Institute of Geophysics, University of Tehran", 17.7, 14.0, null,
        maghribAngle = 4.5, jafariMidnight = true,
    ),
    JAFARI(
        "Shia Ithna-Ashari, Leva Institute, Qum", 16.0, 14.0, null,
        maghribAngle = 4.0, jafariMidnight = true,
    ),
    DUBAI(
        "Dubai, UAE", 18.2, 18.2, null,
    ),
    TURKEY(
        "Turkiye (Diyanet)", 18.0, 17.0, null,
    ),
    FRANCE(
        "France (12°)", 12.0, 12.0, null,
    ),
}