package com.praytracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praytracker.data.TasbihItem
import com.praytracker.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasbihList by viewModel.tasbihList.collectAsState()
    val settingsChanged by viewModel.settings.settingsChanged.collectAsState()
    val selectedId = viewModel.settings.selectedTasbihId
    val showTranslation = viewModel.settings.showTasbihTranslation

    val currentItem = tasbihList.firstOrNull { it.id == selectedId }
        ?: tasbihList.firstOrNull()
        ?: TasbihItem(
            id = 1,
            arabicText = "سُبْحَانَ اللَّهِ",
            englishText = "SubhanAllah (Glory be to Allah)",
            currentCount = 0,
            targetCount = 33
        )

    var showSelectorSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TasbihItem?>(null) }
    var itemToDelete by remember { mutableStateOf<TasbihItem?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val progress = if (currentItem.targetCount > 0) {
        (currentItem.currentCount.toFloat() / currentItem.targetCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "TasbihProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Dhikr Display & Selection Hero Container (Centered, spacious for long/big dhikrs)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSelectorSheet = true }
                .testTag("dhikr_selector_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top affordance badge: Current Dhikr (Click to switch)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT DHIKR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch Dhikr",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Arabic Dhikr (Centered, large display typography, ample room for long dhikrs)
                Text(
                    text = currentItem.arabicText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (currentItem.arabicText.length > 50) 22.sp else 28.sp,
                        lineHeight = if (currentItem.arabicText.length > 50) 34.sp else 42.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                // Dedicated small space for English translation (if enabled in settings and not empty)
                if (showTranslation && currentItem.englishText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentItem.englishText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Generous vertical breathing room above tap counter button for big dhikrs
        Spacer(modifier = Modifier.weight(1f))

        // Main Tap Counter Area positioned lower down
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(255.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.incrementTasbih(currentItem)
                }
                .testTag("tasbih_counter_button")
        ) {
            // Circular Progress Indicator Ring
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(240.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                strokeWidth = 9.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${currentItem.currentCount}",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TARGET: ${if (currentItem.targetCount > 0) currentItem.targetCount else "∞"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    letterSpacing = 1.5.sp
                )
                if (currentItem.targetCount > 0 && currentItem.currentCount >= currentItem.targetCount) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Target Reached",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Controls (Reset & All Dhikrs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.resetTasbih(currentItem) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("reset_tasbih_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Counter",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Counter", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clickable { showSelectorSheet = true }
                    .testTag("change_dhikr_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "All Dhikrs (${tasbihList.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet to choose / add / edit Dhikr
    if (showSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSelectorSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Dhikr",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_custom_dhikr_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Dhikr")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tasbihList) { item ->
                        val isSelected = item.id == currentItem.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.settings.selectedTasbihId = item.id
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        showSelectorSheet = false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.arabicText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.englishText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = (if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "Target: ${item.targetCount} • Current: ${item.currentCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (item.isCustom) {
                                    Row {
                                        IconButton(onClick = { itemToEdit = item }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = { itemToDelete = item }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Custom Dhikr Dialog
    if (showAddDialog) {
        var arabic by remember { mutableStateOf("") }
        var english by remember { mutableStateOf("") }
        var targetStr by remember { mutableStateOf("33") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Dhikr") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = arabic,
                        onValueChange = { arabic = it },
                        label = { Text("Arabic Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = english,
                        onValueChange = { english = it },
                        label = { Text("English / Transliteration") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Count") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetStr.toIntOrNull() ?: 33
                        if (arabic.isNotBlank() || english.isNotBlank()) {
                            viewModel.addCustomTasbih(
                                arabic = arabic.ifBlank { "ذِكْر" },
                                english = english.ifBlank { "Custom Dhikr" },
                                target = target
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Custom Dhikr Dialog
    itemToEdit?.let { item ->
        var arabic by remember { mutableStateOf(item.arabicText) }
        var english by remember { mutableStateOf(item.englishText) }
        var targetStr by remember { mutableStateOf(item.targetCount.toString()) }

        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Edit Dhikr") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = arabic,
                        onValueChange = { arabic = it },
                        label = { Text("Arabic Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = english,
                        onValueChange = { english = it },
                        label = { Text("English / Transliteration") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Count") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetStr.toIntOrNull() ?: item.targetCount
                        viewModel.editTasbih(item, arabic, english, target)
                        itemToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Custom Dhikr Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Dhikr") },
            text = { Text("Are you sure you want to delete \"${item.englishText}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTasbih(item)
                        itemToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
