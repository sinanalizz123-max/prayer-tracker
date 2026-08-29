package com.praytracker.ui.backup

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.praytracker.PrayerTrackerApp
import com.praytracker.R
import com.praytracker.ui.nav.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerTrackerApp
    val backupManager = app.container.backupManager
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (content != null) {
                    val result = backupManager.importJson(content)
                    if (result.success) {
                        app.container.prayerAlarmScheduler.rescheduleAll()
                        snackbar.showSnackbar(context.getString(R.string.backup_imported))
                    } else {
                        snackbar.showSnackbar(
                            context.getString(R.string.backup_import_result_error, result.error ?: "?"),
                        )
                    }
                }
            }
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.more_backup),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text(
                text = "Your backup is a small readable JSON file. Nothing is uploaded anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val uri = backupManager.writeExportToFile()
                        shareJson(context, uri)
                        snackbar.showSnackbar(context.getString(R.string.backup_exported))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_export))
            }
            Spacer(Modifier.size(12.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_import))
            }
            Spacer(Modifier.size(12.dp))
            Text(
                text = "Importing replaces your current prayer history and tasbih phrases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun shareJson(context: Context, uri: android.net.Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, context.getString(R.string.backup_share))
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}