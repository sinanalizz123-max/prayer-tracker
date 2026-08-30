package com.praytracker.ui.history

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.praytracker.R
import com.praytracker.di.AppViewModelProvider
import com.praytracker.prayer.Prayer
import com.praytracker.prayer.PrayerStatus
import com.praytracker.ui.theme.color
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val statusColors = mapOf(
    PrayerStatus.PRAYED to Color(0xFF2E7D56),
    PrayerStatus.DELAYED to Color(0xFFD9A441),
    PrayerStatus.NOT_DID to Color(0xFFB5542F),
    PrayerStatus.NOT_RECORDED to Color(0x33555555),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.history_title)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.size(4.dp))

            MonthSummaryCard(state)

            Spacer(Modifier.size(12.dp))

            MonthHeader(state, viewModel)

            Spacer(Modifier.size(8.dp))

            MonthCalendar(state, viewModel)

            Spacer(Modifier.size(24.dp))
        }
    }

    state.selectedDate?.let { selected ->
        val record = state.records[selected.toString()]
        DayDetailDialog(
            date = selected,
            record = record,
            times = state.selectedTimes,
            onToggle = { prayer ->
                val current = record?.status(prayer) ?: PrayerStatus.NOT_RECORDED
                viewModel.setStatus(selected, prayer, viewModel.nextStatus(current))
            },
            onDismiss = { viewModel.closeDayDetail() },
        )
    }
}

@Composable
private fun MonthSummaryCard(state: HistoryUiState) {
    val month = YearMonth.from(state.monthStart)
    val monthRecords = state.records.values.filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { d -> YearMonth.from(d) == month } ?: false }
    val totalPrayed = monthRecords.sumOf { it.prayedCount() }
    val maxPossible = month.lengthOfMonth() * Prayer.ORDER.size
    val pct = if (maxPossible == 0) 0f else totalPrayed.toFloat() / maxPossible

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.history_summary_prayed, totalPrayed, (pct * 100).toInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MonthHeader(state: HistoryUiState, viewModel: HistoryViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { viewModel.previousMonth() }) {
            Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        TextButton(onClick = { viewModel.today() }) {
            Text(stringResource(R.string.nav_today))
        }
        IconButton(onClick = { viewModel.nextMonth() }) {
            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun MonthCalendar(state: HistoryUiState, viewModel: HistoryViewModel) {
    val month = YearMonth.from(state.monthStart)
    val days = month.lengthOfMonth()
    val firstWeekday = month.atDay(1).dayOfWeek.value // 1 = Monday
    val leading = firstWeekday - 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false,
    ) {
        item(key = "blank_0") { DayCellHeader("Mon") }
        item(key = "blank_1") { DayCellHeader("Tue") }
        item(key = "blank_2") { DayCellHeader("Wed") }
        item(key = "blank_3") { DayCellHeader("Thu") }
        item(key = "blank_4") { DayCellHeader("Fri") }
        item(key = "blank_5") { DayCellHeader("Sat") }
        item(key = "blank_6") { DayCellHeader("Sun") }
        items((1..(leading + days)).toList()) { index ->
            if (index <= leading) {
                DayCell.Empty()
            } else {
                val day = index - leading
                val date = month.atDay(day)
                val record = state.records[date.toString()]
                DayCell.Filled(
                    date = date,
                    record = record,
                    onClick = { viewModel.select(date) },
                )
            }
        }
    }
}

@Composable
private fun DayCellHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private object DayCell {
    @Composable
    fun Empty() {
        Box(Modifier.padding(4.dp))
    }

    @Composable
    fun Filled(
        date: LocalDate,
        record: com.praytracker.data.db.PrayerRecord?,
        onClick: () -> Unit,
    ) {
        val isToday = date == LocalDate.now()
        val bg = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.size(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Prayer.ORDER.forEach { prayer ->
                    val status = record?.status(prayer) ?: PrayerStatus.NOT_RECORDED
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColors[status] ?: Color.Gray),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailDialog(
    date: LocalDate,
    record: com.praytracker.data.db.PrayerRecord?,
    times: com.praytracker.prayer.DailyPrayerTimes?,
    onToggle: (Prayer) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Prayer.ORDER.forEach { prayer ->
                    val status = record?.status(prayer) ?: PrayerStatus.NOT_RECORDED
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggle(prayer) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(prayer.color()),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            prayer.displayName,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            times?.time(prayer)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "–",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            status.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColors[status] ?: Color.Gray,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}