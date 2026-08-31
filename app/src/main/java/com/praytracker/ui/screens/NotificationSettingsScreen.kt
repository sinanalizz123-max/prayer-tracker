package com.praytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praytracker.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val settings = viewModel.settings
    val version by settings.settingsChanged.collectAsState()
    remember(version) { }
    val master = settings.isMasterNotificationEnabled

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Prayer Notifications", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)); NotificationSectionLabel("Prayer alerts") }
            item {
                SettingsToggleRow("Prayer notifications", "Enable or disable all prayer alerts", master) {
                    settings.isMasterNotificationEnabled = it; viewModel.onSettingsChanged()
                }
            }
            if (master) {
                item { NotificationPrayerRow("Fajr", settings.isFajrNotificationEnabled) { settings.isFajrNotificationEnabled = it; viewModel.onSettingsChanged() } }
                item { NotificationPrayerRow("Dhuhr", settings.isDhuhrNotificationEnabled) { settings.isDhuhrNotificationEnabled = it; viewModel.onSettingsChanged() } }
                item { NotificationPrayerRow("Asr", settings.isAsrNotificationEnabled) { settings.isAsrNotificationEnabled = it; viewModel.onSettingsChanged() } }
                item { NotificationPrayerRow("Maghrib", settings.isMaghribNotificationEnabled) { settings.isMaghribNotificationEnabled = it; viewModel.onSettingsChanged() } }
                item { NotificationPrayerRow("Isha", settings.isIshaNotificationEnabled) { settings.isIshaNotificationEnabled = it; viewModel.onSettingsChanged() } }
                item { NotificationSectionLabel("Reminder timing") }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Follow-up reminder: ${settings.reminderDelayMinutes} min after", fontWeight = FontWeight.SemiBold)
                        Slider(value = settings.reminderDelayMinutes.toFloat(), onValueChange = {
                            settings.reminderDelayMinutes = it.toInt(); viewModel.onSettingsChanged()
                        }, valueRange = 0f..45f, steps = 8)
                    }
                }
                item { NotificationSectionLabel("Sound") }
                item {
                    SettingsToggleRow(
                        "Silent alerts",
                        if (settings.isCustomSoundEnabled) "Notifications without sound" else "Use the normal system notification sound",
                        settings.isCustomSoundEnabled
                    ) { settings.isCustomSoundEnabled = it; viewModel.onSettingsChanged() }
                }
                item {
                    Text(
                        "Prayer alerts use Android's notification system. You can also control channel sound and vibration from Android notification settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
                        lineHeight = 18.sp
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun NotificationSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun NotificationPrayerRow(name: String, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Switch(checked = enabled, onCheckedChange = onCheckedChange)
    }
}
