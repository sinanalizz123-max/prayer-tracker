package com.praytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = Emerald90,
    onPrimaryContainer = EmeraldDark,
    secondary = Gold,
    onSecondary = Color.Black,
    background = Sand,
    onBackground = EmeraldDark,
    surface = Color.White,
    onSurface = EmeraldDark,
)

private val DarkColors = darkColorScheme(
    primary = Emerald90,
    onPrimary = EmeraldDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Emerald90,
    secondary = Gold,
    onSecondary = Color.Black,
    background = Color(0xFF0B1B17),
    onBackground = Color(0xFFE6EFEB),
    surface = Color(0xFF132822),
    onSurface = Color(0xFFE6EFEB),
)

@Composable
fun PrayerTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}