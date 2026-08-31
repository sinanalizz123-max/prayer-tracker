package com.praytracker.ui.screens

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val tonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { intent ->
            val uri = intent.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                settings.notificationToneUri = uri.toString()
                viewModel.onSettingsChanged()
            }
        }
    }

    fun launchTonePicker() {
        val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Notification Tone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            if (settings.notificationToneUri.isNotBlank()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(settings.notificationToneUri))
            }
        }
        tonePicker.launch(intent)
    }

    val toneName = remember(settings.notificationToneUri) { toneName(context, settings.notificationToneUri) }

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
                        if (settings.isCustomSoundEnabled) "Notifications without sound" else "Play a sound with prayer alerts",
                        settings.isCustomSoundEnabled
                    ) { settings.isCustomSoundEnabled = it; viewModel.onSettingsChanged() }
                }
                if (!settings.isCustomSoundEnabled) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().clickable { launchTonePicker() }.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Notification tone", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(toneName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
                item {
                    Text(
                        "Pick a tone from the system sound themes. When silent alerts are off, this tone plays at each prayer time.",
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

private fun toneName(context: android.content.Context, uriString: String): String {
    if (uriString.isBlank()) return "Default notification sound"
    return try {
        val ringtone: Ringtone? = RingtoneManager.getRingtone(context, Uri.parse(uriString))
        ringtone?.getTitle(context) ?: "Custom"
    } catch (e: Exception) {
        "Custom"
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
