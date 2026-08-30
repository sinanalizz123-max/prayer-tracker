package com.praytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.praytracker.ui.MainViewModel

@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToRamadan: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "More Features & Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SettingsClickableRow(
                title = "Ramadan Mode",
                subtitle = "Fasting schedule, Suhoor & Iftar countdowns, and Iftar Dua",
                onClick = onNavigateToRamadan
            )
        }

        item {
            SettingsClickableRow(
                title = "Settings",
                subtitle = "Calculation methods, Madhab, manual location search, notifications & theme",
                onClick = onNavigateToSettings
            )
        }

        item {
            SettingsClickableRow(
                title = "About",
                subtitle = "Offline calculations, zero data tracking & privacy principles",
                onClick = onNavigateToAbout
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
