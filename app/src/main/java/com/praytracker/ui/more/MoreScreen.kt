package com.praytracker.ui.more

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.praytracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenQibla: () -> Unit,
    onOpenRamadan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_more)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.size(4.dp))
            MoreItem(stringResource(R.string.more_qibla), Icons.Outlined.LocationOn, onOpenQibla)
            MoreItem(stringResource(R.string.more_ramadan), Icons.Outlined.Star, onOpenRamadan)
            MoreItem(stringResource(R.string.more_settings), Icons.Outlined.Settings, onOpenSettings)
            MoreItem(stringResource(R.string.more_backup), Icons.Outlined.Share, onOpenBackup)
            MoreItem(stringResource(R.string.more_about), Icons.Outlined.Info, onOpenAbout)
            Spacer(Modifier.size(12.dp))
        }
    }
}

@Composable
private fun MoreItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}