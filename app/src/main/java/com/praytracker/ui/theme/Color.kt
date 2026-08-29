package com.praytracker.ui.theme

import androidx.compose.ui.graphics.Color

val Emerald90 = Color(0xFFB6F0DC)
val EmeraldGreen = Color(0xFF14463D)
val EmeraldDark = Color(0xFF0E332C)
val Sand = Color(0xFFF6EFE4)
val Gold = Color(0xFFD9A441)

val PRAYER_FAJR = Color(0xFF44647E)
val PRAYER_DHUHR = Color(0xFF2E7D56)
val PRAYER_ASR = Color(0xFF8A6D2F)
val PRAYER_MAGHRIB = Color(0xFFB5542F)
val PRAYER_ISHA = Color(0xFF3F4E9E)

fun com.praytracker.prayer.Prayer.color(): Color = when (this) {
    com.praytracker.prayer.Prayer.FAJR -> PRAYER_FAJR
    com.praytracker.prayer.Prayer.DHUHR -> PRAYER_DHUHR
    com.praytracker.prayer.Prayer.ASR -> PRAYER_ASR
    com.praytracker.prayer.Prayer.MAGHRIB -> PRAYER_MAGHRIB
    com.praytracker.prayer.Prayer.ISHA -> PRAYER_ISHA
}