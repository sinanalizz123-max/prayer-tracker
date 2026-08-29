package com.praytracker.ui.tasbih

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.praytracker.R
import com.praytracker.data.db.TasbihEntity
import com.praytracker.di.AppViewModelProvider
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    viewModel: TasbihViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TasbihEntity?>(null) }
    val context = LocalContext.current

    val selected = state.items.firstOrNull { it.id == state.selectedId }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_tasbih)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.tasbih_add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.size(8.dp))

            if (selected != null) {
                BigCounter(
                    entity = selected,
                    onIncrement = {
                        viewModel.incrementSelected()
                        if (state.settings.tasbihHapticsEnabled) {
                            vibrate(context)
                        }
                    },
                    onReset = { viewModel.resetSelected() },
                )
            } else {
                Text(
                    text = stringResource(R.string.tasbih_add),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Spacer(Modifier.size(16.dp))

            Text(
                stringResource(R.string.nav_tasbih).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.id }) { item ->
                    DhikrChip(
                        entity = item,
                        selected = item.id == state.selectedId,
                        onClick = { viewModel.select(item.id) },
                        onEdit = { editing = item },
                        onDelete = { viewModel.delete(item.id) },
                    )
                }
            }

            Spacer(Modifier.size(24.dp))
        }
    }

    if (showAddDialog) {
        EditDhikrDialog(
            title = stringResource(R.string.tasbih_add),
            initial = null,
            onSave = { arabic, translation, target ->
                viewModel.add(arabic, translation, target)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editing?.let { entity ->
        EditDhikrDialog(
            title = "Edit",
            initial = entity,
            onSave = { arabic, translation, target ->
                viewModel.update(entity.copy(arabic = arabic.trim(), translation = translation, target = target))
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun BigCounter(
    entity: TasbihEntity,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
) {
    val progress = if (entity.target <= 0) 0f else min(entity.count, entity.target).toFloat() / entity.target

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onIncrement),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(220.dp)) {
                    val stroke = 12.dp.toPx()
                    drawArc(
                        color = MaterialTheme.colorScheme.surface,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = MaterialTheme.colorScheme.primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = entity.count.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.tasbih_target, entity.target),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            Text(
                text = entity.arabic,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            entity.translation?.let {
                Spacer(Modifier.size(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.size(16.dp))

            FilledTonalButton(onClick = onReset) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.tasbih_reset))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.tasbih_reset))
            }
        }
    }
}

@Composable
private fun DhikrChip(
    entity: TasbihEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                Text(
                    text = entity.arabic,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = "${entity.count} / ${entity.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Create,
                    contentDescription = "Edit",
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EditDhikrDialog(
    title: String,
    initial: TasbihEntity?,
    onSave: (arabic: String, translation: String?, target: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var arabic by remember { mutableStateOf(initial?.arabic ?: "") }
    var translation by remember { mutableStateOf(initial?.translation ?: "") }
    var target by remember { mutableStateOf((initial?.target ?: 33).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = arabic,
                    onValueChange = { arabic = it },
                    label = { Text("Arabic") },
                    singleLine = true,
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text(stringResource(R.string.tasbih_name_hint)) },
                    singleLine = true,
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(R.string.tasbih_target_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val t = target.toIntOrNull() ?: 33
                    if (arabic.isNotBlank()) {
                        onSave(arabic, translation, t)
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun vibrate(context: android.content.Context) {
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }
}