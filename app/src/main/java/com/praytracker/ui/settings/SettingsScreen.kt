package com.praytracker.ui.settings

import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.praytracker.R
import com.praytracker.data.settings.LocationMode
import com.praytracker.data.settings.Theme
import com.praytracker.di.AppViewModelProvider
import com.praytracker.prayer.CalcMethod
import com.praytracker.prayer.HighLatRule
import com.praytracker.prayer.Madhab
import com.praytracker.prayer.Prayer
import com.praytracker.ui.nav.ScreenScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showMethodPicker by remember { mutableStateOf(false) }
    var showMadhabPicker by remember { mutableStateOf(false) }
    var showHighLatPicker by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        onSettings = { /* no-op */ },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Calculation
            SettingsSection(stringResource(R.string.settings_calculation)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PickerChip(
                        label = stringResource(R.string.settings_method),
                        value = calcMethodDisplay(settings),
                        onClick = { showMethodPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                    PickerChip(
                        label = stringResource(R.string.settings_madhab),
                        value = settings.madhab,
                        onClick = { showMadhabPicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                PickerChip(
                    label = stringResource(R.string.settings_high_lat),
                    value = settings.highLatRule,
                    onClick = { showHighLatPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.settings_adjustments),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Prayer.ORDER.forEach { prayer ->
                    AdjustRow(
                        label = prayer.displayName,
                        value = settings.adjustmentFor(prayer),
                        onChanged = { viewModel.update { s -> applyAdjustment(s, prayer, it) } },
                    )
                }
                AdjustRow(
                    label = stringResource(R.string.settings_hijri_offset),
                    value = settings.hijriOffsetDays,
                    onChanged = { viewModel.update { s -> s.copy(hijriOffsetDays = it) } },
                )
            }

            // Location
            SettingsSection(stringResource(R.string.settings_location)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.settings_location_auto),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = settings.locationMode == LocationMode.AUTO,
                        onCheckedChange = { auto ->
                            viewModel.update {
                                it.copy(locationMode = if (auto) LocationMode.AUTO else LocationMode.MANUAL)
                            }
                        },
                    )
                }
                if (settings.locationMode == LocationMode.AUTO) {
                    OutlinedButton(onClick = { scope.launch { viewModel.refreshLocation() } }) {
                        Text("Update now")
                    }
                }
                Text(
                    text = settings.locationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Notifications
            SettingsSection(stringResource(R.string.settings_notifications)) {
                ToggleRow(
                    label = stringResource(R.string.settings_enabled),
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { viewModel.update { s -> s.copy(notificationsEnabled = it) } },
                )
                if (settings.notificationsEnabled) {
                    Prayer.ORDER.forEach { prayer ->
                        ToggleRow(
                            label = prayer.displayName,
                            checked = settings.notifEnabledFor(prayer),
                            onCheckedChange = {
                                viewModel.update { s -> applyPrayerNotif(s, prayer, it) }
                            },
                        )
                    }
                    ToggleRow(
                        label = stringResource(R.string.settings_reminder),
                        checked = settings.reminderEnabled,
                        onCheckedChange = { viewModel.update { s -> s.copy(reminderEnabled = it) } },
                    )
                    if (settings.reminderEnabled) {
                        AdjustRow(
                            label = stringResource(R.string.settings_reminder_minutes),
                            value = settings.reminderMinutes,
                            onChanged = { viewModel.update { s -> s.copy(reminderMinutes = it.coerceIn(1, 60)) } },
                        )
                    }
                    ToggleRow(
                        label = stringResource(R.string.settings_sound),
                        checked = settings.notifSoundEnabled,
                        onCheckedChange = { viewModel.update { s -> s.copy(notifSoundEnabled = it) } },
                    )
                    ToggleRow(
                        label = stringResource(R.string.settings_vibrate),
                        checked = settings.notifVibrateEnabled,
                        onCheckedChange = { viewModel.update { s -> s.copy(notifVibrateEnabled = it) } },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarm = context.getSystemService(android.app.AlarmManager::class.java)
                        val canExact = alarm.canScheduleExactAlarms()
                        if (!canExact) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:${context.packageName}"),
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.settings_exact_alarms))
                            }
                        }
                    }
                }
            }

            // Appearance
            SettingsSection(stringResource(R.string.settings_appearance)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        Pair(Theme.SYSTEM, stringResource(R.string.settings_theme_system)),
                        Pair(Theme.LIGHT, stringResource(R.string.settings_theme_light)),
                        Pair(Theme.DARK, stringResource(R.string.settings_theme_dark)),
                    ).forEach { (theme, label) ->
                        FilterChip(
                            selected = settings.theme == theme,
                            onClick = { viewModel.update { s -> s.copy(theme = theme) } },
                            label = { Text(label) },
                        )
                    }
                }
            }

            // Tasbih
            SettingsSection(stringResource(R.string.settings_tasbih)) {
                ToggleRow(
                    label = stringResource(R.string.settings_tasbih_haptics),
                    checked = settings.tasbihHapticsEnabled,
                    onCheckedChange = { viewModel.update { s -> s.copy(tasbihHapticsEnabled = it) } },
                )
            }

            // Security
            SettingsSection(stringResource(R.string.settings_security)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.settings_app_lock),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (settings.appLockPasscodeHash == null) {
                                    showPasscodeDialog = true
                                } else {
                                    viewModel.setAppLockEnabled(true)
                                }
                            } else {
                                viewModel.setAppLockEnabled(false)
                            }
                        },
                    )
                }
            }

            // Backup + About links
            OutlinedButton(
                onClick = onOpenBackup,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_backup))
            }
        }
    }

    if (showMethodPicker) {
        ListPickerDialog(
            title = stringResource(R.string.settings_method),
            options = CalcMethod.entries.map { it.name },
            selected = settings.calcMethod,
            onSelect = { viewModel.update { s -> s.copy(calcMethod = it) }; showMethodPicker = false },
            onDismiss = { showMethodPicker = false },
        )
    }
    if (showMadhabPicker) {
        ListPickerDialog(
            title = stringResource(R.string.settings_madhab),
            options = Madhab.entries.map { it.name },
            selected = settings.madhab,
            onSelect = { viewModel.update { s -> s.copy(madhab = it) }; showMadhabPicker = false },
            onDismiss = { showMadhabPicker = false },
        )
    }
    if (showHighLatPicker) {
        ListPickerDialog(
            title = stringResource(R.string.settings_high_lat),
            options = HighLatRule.entries.map { it.name },
            selected = settings.highLatRule,
            onSelect = { viewModel.update { s -> s.copy(highLatRule = it) }; showHighLatPicker = false },
            onDismiss = { showHighLatPicker = false },
        )
    }
    if (showPasscodeDialog) {
        var pin by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }
        val textMismatch = stringResource(R.string.lock_mismatch)
        AlertDialog(
            onDismissRequest = { showPasscodeDialog = false },
            title = { Text(stringResource(R.string.lock_set_passcode)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                        label = { Text("Passcode (4 digits)") },
                        singleLine = true,
                    )
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(R.string.lock_confirm_passcode)) },
                        singleLine = true,
                    )
                    if (error.isNotEmpty()) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pin.length != 4 || confirm.length != 4) {
                            error = "Enter a 4-digit passcode."
                        } else if (pin != confirm) {
                            error = textMismatch
                        } else {
                            viewModel.setPasscode(pin)
                            showPasscodeDialog = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasscodeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun PickerChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AdjustRow(
    label: String,
    value: Int,
    onChanged: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("+$value min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { onChanged(value - 1) }, enabled = value > -20) {
            Icon(Icons.Outlined.Remove, contentDescription = "Decrease")
        }
        IconButton(onClick = { onChanged(value + 1) }, enabled = value < 20) {
            Icon(Icons.Outlined.Add, contentDescription = "Increase")
        }
    }
}

@Composable
private fun ListPickerDialog(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Text(
                            option,
                            Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun applyAdjustment(settings: com.praytracker.data.settings.Settings, prayer: Prayer, value: Int) =
    when (prayer) {
        Prayer.FAJR -> settings.copy(fajrAdjustment = value)
        Prayer.DHUHR -> settings.copy(dhuhrAdjustment = value)
        Prayer.ASR -> settings.copy(asrAdjustment = value)
        Prayer.MAGHRIB -> settings.copy(maghribAdjustment = value)
        Prayer.ISHA -> settings.copy(ishaAdjustment = value)
    }

private fun applyPrayerNotif(settings: com.praytracker.data.settings.Settings, prayer: Prayer, enabled: Boolean) =
    when (prayer) {
        Prayer.FAJR -> settings.copy(fajrNotifEnabled = enabled)
        Prayer.DHUHR -> settings.copy(dhuhrNotifEnabled = enabled)
        Prayer.ASR -> settings.copy(asrNotifEnabled = enabled)
        Prayer.MAGHRIB -> settings.copy(maghribNotifEnabled = enabled)
        Prayer.ISHA -> settings.copy(ishaNotifEnabled = enabled)
    }

private fun calcMethodDisplay(settings: com.praytracker.data.settings.Settings): String {
    val method = CalcMethod.entries.firstOrNull { it.name == settings.calcMethod } ?: CalcMethod.MWL
    return method.name
}