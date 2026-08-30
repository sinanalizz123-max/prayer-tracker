package com.praytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praytracker.ui.MainViewModel
import com.praytracker.ui.components.HijriCalendarBottomSheet
import com.praytracker.ui.components.LocationPickerBottomSheet
import com.praytracker.ui.theme.ActivePrayerPurple
import com.praytracker.ui.theme.EmeraldGreen
import com.praytracker.ui.theme.EmeraldGreenContainer
import com.praytracker.util.PrayerCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRamadan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val schedule by viewModel.prayerSchedule.collectAsState()
    val nextPrayer by viewModel.nextPrayerInfo.collectAsState()
    val hijri by viewModel.hijriDate.collectAsState()
    val activePrayer by viewModel.currentActivePrayer.collectAsState()
    val isDetecting by viewModel.isDetectingLocation.collectAsState()
    val locationError by viewModel.locationError.collectAsState()

    var showLocationPickerSheet by remember { mutableStateOf(false) }
    var showHijriCalendarSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Location and Action Row (Tap location to search any town worldwide)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showLocationPickerSheet = true }
                        .testTag("location_picker_trigger")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = schedule.locationName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Change Location",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (isDetecting) {
                                Text(
                                    text = "Detecting location...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.detectLocation() },
                        modifier = Modifier.testTag("refresh_location_button")
                    ) {
                        if (isDetecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldGreen
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Location",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Location error banner
        if (locationError != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = locationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Date Card (Hijri & Gregorian) - Tap to view full Hijri Calendar & edit date
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHijriCalendarSheet = true }
                    .testTag("date_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = hijri.formattedEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Open Hijri Calendar",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = schedule.formattedGregorianDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    Text(
                        text = hijri.formattedAr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Next Prayer Hero Card (Emerald Green Banner matching screenshot)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EmeraldGreenContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("next_prayer_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NEXT PRAYER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 2.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = nextPrayer?.name ?: "--",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = nextPrayer?.formattedTime ?: "--:--",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Countdown Pill Badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.28f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val mins = nextPrayer?.countdownMinutes ?: 0
                            val secs = nextPrayer?.countdownSeconds ?: 0
                            val countdownText = if (mins >= 60) {
                                "- in ${mins / 60}h ${mins % 60}m ${secs}s"
                            } else {
                                "- in ${mins}m ${secs}s"
                            }
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Daily Prayer List Items (Highlight active prayer in Purple matching screenshot)
        items(schedule.list) { item ->
            val isCurrentActive = activePrayer.equals(item.name, ignoreCase = true)
            PrayerItemRow(
                item = item,
                isActive = isCurrentActive
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Location Picker Bottom Sheet (Auto-suggest town/city search)
    if (showLocationPickerSheet) {
        LocationPickerBottomSheet(
            viewModel = viewModel,
            onDismiss = { showLocationPickerSheet = false }
        )
    }

    // Hijri Calendar Bottom Sheet (Monthly interactive view & moon sighting edit)
    if (showHijriCalendarSheet) {
        HijriCalendarBottomSheet(
            viewModel = viewModel,
            onDismiss = { showHijriCalendarSheet = false }
        )
    }
}

@Composable
fun PrayerItemRow(
    item: PrayerCalculator.PrayerItem,
    isActive: Boolean
) {
    // If active prayer (like Dhuhr in screenshot), render vivid Purple card with white text!
    val containerColor = if (isActive) ActivePrayerPurple else MaterialTheme.colorScheme.surface
    val contentColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface

    val icon: ImageVector = when (item.type) {
        "FAJR" -> Icons.Default.Nightlight
        "SUNRISE" -> Icons.Default.WbSunny
        "DHUHR" -> Icons.Default.WbSunny
        "ASR" -> Icons.Default.WbCloudy
        "MAGHRIB" -> Icons.Default.WbTwilight
        "ISHA" -> Icons.Default.Nightlight
        else -> Icons.Default.Schedule
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${item.type.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Color.White.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.name,
                        tint = if (isActive) Color.White else EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        if (item.isSunrise) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(Sunrise)",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Text(
                text = item.formattedTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

