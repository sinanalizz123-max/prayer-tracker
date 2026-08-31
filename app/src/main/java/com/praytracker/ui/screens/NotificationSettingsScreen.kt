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
import androidx.compose.material3.FilterChip
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
fun NotificationSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings = viewModel.settings
    val settingsVersion by settings.settingsChanged.collectAsState()
    remember(settingsVersion) { }

    val master = settings.isMasterNotificationEnabled
    val beforeEnabled = settings.isPreReminderEnabled
    val beforeDelay = settings.preReminderMinutes
    val afterDelay = settings.reminderDelayMinutes
    val vibration = settings.isNotificationVibrationEnabled
    val silent = settings.isCustomSoundEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                Spacer(Modifier.height(8.dp))
                NotificationSectionLabel("General")
            }
            item {
                SettingsToggleRow(
                    title = "Prayer notifications",
                    subtitle = "Enable or disable all prayer alerts",
                    checked = master,
                    onCheckedChange = {
                        settings.isMasterNotificationEnabled = it
                        viewModel.onSettingsChanged()
                    }
                )
            }

            if (master) {
                item {
                    NotificationSectionLabel("Prayer alerts")
                }
                item { NotificationPrayerRow("Fajr", settings.isFajrNotificationEnabled) {
                    settings.isFajrNotificationEnabled = it; viewModel.onSettingsChanged()
                } }
                item { NotificationPrayerRow("Dhuhr", settings.isDhuhrNotificationEnabled) {
                    settings.isDhuhrNotificationEnabled = it; viewModel.onSettingsChanged()
                } }
                item { NotificationPrayerRow("Asr", settings.isAsrNotificationEnabled) {
                    settings.isAsrNotificationEnabled = it; viewModel.onSettingsChanged()
                } }
                item { NotificationPrayerRow("Maghrib", settings.isMaghribNotificationEnabled) {
                    settings.isMaghribNotificationEnabled = it; viewModel.onSettingsChanged()
                } }
                item { NotificationPrayerRow("Isha", settings.isIshaNotificationEnabled) {
                    settings.isIshaNotificationEnabled = it; viewModel.onSettingsChanged()
                } }

                item {
                    NotificationSectionLabel("Reminder timing")
                }
                item {
                    SettingsToggleRow(
                        title = "Before-prayer reminder",
                        subtitle = if (beforeEnabled) "$beforeDelay minutes before each prayer" else "Disabled",
                        checked = beforeEnabled,
                        onCheckedChange = {
                            settings.isPreReminderEnabled = it
                            viewModel.onSettingsChanged()
                        }
                    )
                }
                if (beforeEnabled) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Before reminder: $beforeDelay min", fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = beforeDelay.toFloat(),
                                onValueChange = {
                                    settings.preReminderMinutes = it.toInt()
                                    viewModel.onSettingsChanged()
                                },
                                valueRange = 5f..60f,
                                steps = 10
                            )
                        }
                    }
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Follow-up: $afterDelay min after", fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = afterDelay.toFloat(),
                            onValueChange = {
                                settings.reminderDelayMinutes = it.toInt()
                                viewModel.onSettingsChanged()
                            },
                            valueRange = 0f..45f,
                            steps = 8
                        )
                    }
                }

                item {
                    NotificationSectionLabel("Sound & vibration")
                }
                item {
                    SettingsToggleRow(
                        title = "Vibration",
                        subtitle = if (vibration) "Use the device's notification vibration" else "No notification vibration",
                        checked = vibration,
                        onCheckedChange = {
                            settings.isNotificationVibrationEnabled = it
                            viewModel.onSettingsChanged()
                        }
                    )
                }
                item {
                    SettingsToggleRow(
                        title = "Silent alerts",
                        subtitle = if (silent) "Notifications without sound" else "Use the normal notification sound",
                        checked = silent,
                        onCheckedChange = {
                            settings.isCustomSoundEnabled = it
                            viewModel.onSettingsChanged()
                        }
                    )
                }

                item {
                    Text(
                        text = "Android controls the available notification sounds. This app keeps the choice simple: normal system alert or silent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        lineHeight = 18.sp
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NotificationSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotificationPrayerRow(name: String, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Switch(checked = enabled, onCheckedChange = onCheckedChange)
    }
}
