package com.praytracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praytracker.ui.MainViewModel
import com.praytracker.ui.components.HijriCalendarBottomSheet
import com.praytracker.ui.components.LocationPickerBottomSheet
import com.praytracker.ui.components.rememberLocationPermissionDetector
import com.praytracker.util.PrayerCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val settings = viewModel.settings

    // This screen renders many rows by reading settings through plain getters at
    // composition time, so subscribe to the change counter here. When any setting
    // is written, this scope recomposes and re-reads those getters immediately.
    val settingsChanged by viewModel.settings.settingsChanged.collectAsState()
    remember(settingsChanged) { }

    var showCalcMethodDialog by remember { mutableStateOf(false) }
    var showMadhabDialog by remember { mutableStateOf(false) }
    var showHighLatDialog by remember { mutableStateOf(false) }
    var showManualAdjustDialog by remember { mutableStateOf(false) }
    var showLocationPickerSheet by remember { mutableStateOf(false) }
    var showHijriCalendarSheet by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    var refreshKey by remember { mutableIntStateOf(0) }

    val detectLocation = rememberLocationPermissionDetector(
        onGranted = { viewModel.detectLocation() },
        onDenied = { settings.isAutomaticLocation = false }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SettingsHeader(title = "Location & City Selection", icon = Icons.Default.Place)
            }

            item {
                SettingsClickableRow(
                    title = "Select Town or City",
                    subtitle = "Search any global town/city with auto-suggestions",
                    onClick = { showLocationPickerSheet = true }
                )
            }

            item {
                SettingsToggleRow(
                    title = "Automatic GPS Location",
                    subtitle = if (settings.isAutomaticLocation) "Detect automatically via GPS" else "Custom/Selected town active",
                    checked = settings.isAutomaticLocation,
                    onCheckedChange = { isAuto ->
                        settings.isAutomaticLocation = isAuto
                        if (isAuto) {
                            detectLocation()
                        } else {
                            showLocationPickerSheet = true
                        }
                        refreshKey++
                    }
                )
            }

            item {
                SettingsClickableRow(
                    title = "Active Location Coordinates",
                    subtitle = "${settings.locationName} (${String.format("%.4f", settings.latitude)}, ${String.format("%.4f", settings.longitude)})",
                    onClick = { showLocationPickerSheet = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsHeader(title = "Prayer Times Calculation", icon = Icons.Default.Schedule)
            }

            item {
                SettingsClickableRow(
                    title = "Calculation Method",
                    subtitle = PrayerCalculator.CALCULATION_METHOD_NAMES.getOrElse(settings.calculationMethod) { "Default" },
                    onClick = { showCalcMethodDialog = true }
                )
            }

            item {
                SettingsClickableRow(
                    title = "Madhab (Asr shadow)",
                    subtitle = PrayerCalculator.MADHAB_NAMES.getOrElse(settings.madhab) { "Shafi'i, Maliki, Hanbali (Standard)" },
                    onClick = { showMadhabDialog = true }
                )
            }

            item {
                SettingsClickableRow(
                    title = "High-Latitude Rule",
                    subtitle = PrayerCalculator.HIGH_LATITUDE_RULE_NAMES.getOrElse(settings.highLatitudeRule) { "Middle of the Night" },
                    onClick = { showHighLatDialog = true }
                )
            }

            item {
                SettingsClickableRow(
                    title = "Manual Adjustments",
                    subtitle = "Fajr: ${formatOffset(settings.adjustmentFajr)}, Dhuhr: ${formatOffset(settings.adjustmentDhuhr)}, Asr: ${formatOffset(settings.adjustmentAsr)}, Maghrib: ${formatOffset(settings.adjustmentMaghrib)}, Isha: ${formatOffset(settings.adjustmentIsha)}",
                    onClick = { showManualAdjustDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsHeader(title = "Date & Hijri Calendar", icon = Icons.Default.DateRange)
            }

            item {
                SettingsClickableRow(
                    title = "Open Hijri Calendar",
                    subtitle = "View monthly lunar calendar, Islamic events & dates",
                    onClick = { showHijriCalendarSheet = true }
                )
            }

            item {
                SettingsToggleRow(
                    title = "Arabic Numerals (١، ٢، ٣)",
                    subtitle = if (settings.useArabicNumerals) "Displaying dates in Eastern Arabic digits" else "Displaying standard Western digits (1, 2, 3)",
                    checked = settings.useArabicNumerals,
                    onCheckedChange = {
                        settings.useArabicNumerals = it
                        refreshKey++
                    }
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Moon Sighting / Hijri Adjustment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Adjust the lunar calendar by ±1 or ±2 days if local moon sighting differs from calculated dates:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(-2, -1, 0, 1, 2).forEach { offset ->
                                val isSelected = settings.hijriAdjustment == offset
                                val label = when {
                                    offset == 0 -> "0 (Default)"
                                    offset > 0 -> "+$offset d"
                                    else -> "$offset d"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setHijriAdjustment(offset)
                                        refreshKey++
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsHeader(title = "Notifications & Reminders", icon = Icons.Default.Notifications)
            }

            item {
                SettingsToggleRow(
                    title = "Enable Notifications",
                    subtitle = "Receive reminders at prayer times",
                    checked = settings.isMasterNotificationEnabled,
                    onCheckedChange = {
                        settings.isMasterNotificationEnabled = it
                        viewModel.onSettingsChanged()
                        refreshKey++
                    }
                )
            }

            if (settings.isMasterNotificationEnabled) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Individual Prayer Reminders",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            PrayerReminderToggle("Fajr", settings.isFajrNotificationEnabled) {
                                settings.isFajrNotificationEnabled = it
                                viewModel.onSettingsChanged()
                                refreshKey++
                            }
                            PrayerReminderToggle("Dhuhr", settings.isDhuhrNotificationEnabled) {
                                settings.isDhuhrNotificationEnabled = it
                                viewModel.onSettingsChanged()
                                refreshKey++
                            }
                            PrayerReminderToggle("Asr", settings.isAsrNotificationEnabled) {
                                settings.isAsrNotificationEnabled = it
                                viewModel.onSettingsChanged()
                                refreshKey++
                            }
                            PrayerReminderToggle("Maghrib", settings.isMaghribNotificationEnabled) {
                                settings.isMaghribNotificationEnabled = it
                                viewModel.onSettingsChanged()
                                refreshKey++
                            }
                            PrayerReminderToggle("Isha", settings.isIshaNotificationEnabled) {
                                settings.isIshaNotificationEnabled = it
                                viewModel.onSettingsChanged()
                                refreshKey++
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Follow-up Reminder: ${settings.reminderDelayMinutes} min after",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = settings.reminderDelayMinutes.toFloat(),
                                onValueChange = {
                                    settings.reminderDelayMinutes = it.toInt()
                                    viewModel.onSettingsChanged()
                                    refreshKey++
                                },
                                valueRange = 0f..45f,
                                steps = 8
                            )
                        }
                    }
                }

                item {
                    SettingsToggleRow(
                        title = "Silent Alerts",
                        subtitle = if (settings.isCustomSoundEnabled) "Prayer alerts silent (no sound/vibration)" else "Default notification sound & vibration",
                        checked = settings.isCustomSoundEnabled,
                        onCheckedChange = {
                            settings.isCustomSoundEnabled = it
                            refreshKey++
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsHeader(title = "Tasbih Preferences", icon = Icons.Default.TouchApp)
            }

            item {
                SettingsToggleRow(
                    title = "Show Dhikr Translation",
                    subtitle = if (settings.showTasbihTranslation) "English translation & meaning visible" else "Hidden (Arabic only)",
                    checked = settings.showTasbihTranslation,
                    onCheckedChange = {
                        settings.showTasbihTranslation = it
                        refreshKey++
                    }
                )
            }

            item {
                SettingsToggleRow(
                    title = "Haptic Vibration",
                    subtitle = "Vibrate subtly on each count",
                    checked = settings.isHapticFeedbackEnabled,
                    onCheckedChange = {
                        settings.isHapticFeedbackEnabled = it
                        refreshKey++
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsHeader(title = "Appearance", icon = Icons.Default.Palette)
            }

            item {
                SettingsClickableRow(
                    title = "Theme",
                    subtitle = settings.appTheme.replaceFirstChar { it.uppercase() },
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                SettingsHeader(title = "About", icon = Icons.Default.Info)
            }

            item {
                SettingsClickableRow(
                    title = "About Prayer Times",
                    subtitle = "Version 1.0.1 • Offline-first, Private",
                    onClick = onNavigateToAbout
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showLocationPickerSheet) {
        LocationPickerBottomSheet(
            viewModel = viewModel,
            onDismiss = { showLocationPickerSheet = false }
        )
    }

    if (showHijriCalendarSheet) {
        HijriCalendarBottomSheet(
            viewModel = viewModel,
            onDismiss = { showHijriCalendarSheet = false }
        )
    }

    if (showCalcMethodDialog) {
        AlertDialog(
            onDismissRequest = { showCalcMethodDialog = false },
            title = { Text("Select Calculation Method") },
            text = {
                LazyColumn {
                    items(PrayerCalculator.CALCULATION_METHOD_NAMES.size) { index ->
                        val name = PrayerCalculator.CALCULATION_METHOD_NAMES[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settings.calculationMethod = index
                                    viewModel.onSettingsChanged()
                                    showCalcMethodDialog = false
                                    refreshKey++
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.calculationMethod == index,
                                onClick = {
                                    settings.calculationMethod = index
                                    viewModel.onSettingsChanged()
                                    showCalcMethodDialog = false
                                    refreshKey++
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalcMethodDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showMadhabDialog) {
        AlertDialog(
            onDismissRequest = { showMadhabDialog = false },
            title = { Text("Select Madhab") },
            text = {
                Column {
                    PrayerCalculator.MADHAB_NAMES.forEachIndexed { index, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settings.madhab = index
                                    viewModel.onSettingsChanged()
                                    showMadhabDialog = false
                                    refreshKey++
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.madhab == index,
                                onClick = {
                                    settings.madhab = index
                                    viewModel.onSettingsChanged()
                                    showMadhabDialog = false
                                    refreshKey++
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMadhabDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showHighLatDialog) {
        AlertDialog(
            onDismissRequest = { showHighLatDialog = false },
            title = { Text("High-Latitude Rule") },
            text = {
                Column {
                    PrayerCalculator.HIGH_LATITUDE_RULE_NAMES.forEachIndexed { index, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settings.highLatitudeRule = index
                                    viewModel.onSettingsChanged()
                                    showHighLatDialog = false
                                    refreshKey++
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.highLatitudeRule == index,
                                onClick = {
                                    settings.highLatitudeRule = index
                                    viewModel.onSettingsChanged()
                                    showHighLatDialog = false
                                    refreshKey++
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHighLatDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showManualAdjustDialog) {
        var fajrAdj by remember { mutableIntStateOf(settings.adjustmentFajr) }
        var dhuhrAdj by remember { mutableIntStateOf(settings.adjustmentDhuhr) }
        var asrAdj by remember { mutableIntStateOf(settings.adjustmentAsr) }
        var maghribAdj by remember { mutableIntStateOf(settings.adjustmentMaghrib) }
        var ishaAdj by remember { mutableIntStateOf(settings.adjustmentIsha) }

        AlertDialog(
            onDismissRequest = { showManualAdjustDialog = false },
            title = { Text("Manual Prayer Offsets (Minutes)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OffsetStepperRow("Fajr", fajrAdj) { fajrAdj = it }
                    OffsetStepperRow("Dhuhr", dhuhrAdj) { dhuhrAdj = it }
                    OffsetStepperRow("Asr", asrAdj) { asrAdj = it }
                    OffsetStepperRow("Maghrib", maghribAdj) { maghribAdj = it }
                    OffsetStepperRow("Isha", ishaAdj) { ishaAdj = it }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            fajrAdj = 0; dhuhrAdj = 0; asrAdj = 0; maghribAdj = 0; ishaAdj = 0
                        }
                    ) {
                        Text("Reset to 0")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settings.adjustmentFajr = fajrAdj
                        settings.adjustmentDhuhr = dhuhrAdj
                        settings.adjustmentAsr = asrAdj
                        settings.adjustmentMaghrib = maghribAdj
                        settings.adjustmentIsha = ishaAdj
                        viewModel.onSettingsChanged()
                        showManualAdjustDialog = false
                        refreshKey++
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAdjustDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settings.appTheme = key
                                    showThemeDialog = false
                                    refreshKey++
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.appTheme == key,
                                onClick = {
                                    settings.appTheme = key
                                    showThemeDialog = false
                                    refreshKey++
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PrayerReminderToggle(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun OffsetStepperRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                text = if (value > 0) "+$value min" else "$value min",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { onValueChange(value + 1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun formatOffset(offset: Int): String {
    return if (offset > 0) "+$offset" else "$offset"
}
