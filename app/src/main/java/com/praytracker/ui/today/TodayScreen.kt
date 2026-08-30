package com.praytracker.ui.today

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.praytracker.R
import com.praytracker.data.settings.Settings
import com.praytracker.di.AppViewModelProvider
import com.praytracker.hijri.HijriDateUtil
import com.praytracker.prayer.Prayer
import com.praytracker.prayer.PrayerStatus
import com.praytracker.ui.theme.color
import com.praytracker.util.countdownText
import com.praytracker.util.currentTimeState
import com.praytracker.util.nextPrayer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by currentTimeState()
    val context = LocalContext.current

    val shownDate = LocalDate.now().plusDays(state.dayOffset.toLong())
    val times = state.times
    val record = state.record
    val next = if (state.dayOffset == 0 && times != null) nextPrayer(now, times) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.size(8.dp))

        // Location + settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.settings.locationLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "%.4f, %.4f".format(state.settings.latitude, state.settings.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        }

        // Date header with day navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { viewModel.shiftDay(-1) }) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = shownDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    style = MaterialTheme.typography.titleSmall,
                )
                val hijri = HijriDateUtil.fromGregorian(shownDate.plusDays(state.settings.hijriOffsetDays.toLong()))
                Text(
                    text = "${hijri.day} ${hijri.monthName} ${hijri.year}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                if (state.dayOffset != 0) {
                    IconButton(onClick = { viewModel.resetDay() }) {
                        Text("Today", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = { viewModel.shiftDay(1) }) {
                    Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "Next day")
                }
            }
        }

        Spacer(Modifier.size(8.dp))

        // Next prayer card for today
        if (next != null && times != null) {
            val target = times.localDateTime(next)
            NextPrayerCard(
                prayer = next,
                countdown = countdownText(now, target) ?: "–",
                timeText = target?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "",
            )
        }

        Spacer(Modifier.size(8.dp))

        // Progress
        val prayedCount = record?.prayedCount() ?: 0
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.today_daily_progress), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.today_of_5, prayedCount),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.size(8.dp))
                LinearProgressIndicator(
                    progress = { prayedCount / 5f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.size(12.dp))

        // Prayer rows (with Sunrise)
        PrayerRows(state.settings, times, record, state.dayOffset, next, viewModel)

        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun NextPrayerCard(prayer: Prayer, countdown: String, timeText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = prayer.color()),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.today_next_prayer).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "${prayer.displayName} · $timeText",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = countdown,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Light,
                color = Color.White,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun PrayerRows(
    settings: Settings,
    times: com.praytracker.prayer.DailyPrayerTimes?,
    record: com.praytracker.data.db.PrayerRecord?,
    dayOffset: Int,
    nextPrayer: Prayer?,
    viewModel: TodayViewModel,
    modifier: Modifier = Modifier,
) {
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Prayer.ORDER.forEach { prayer ->
            val status = record?.status(prayer) ?: PrayerStatus.NOT_RECORDED
            val isNext = dayOffset == 0 && nextPrayer == prayer
            PrayerRow(
                prayer = prayer,
                time = times?.time(prayer)?.format(timeFormat) ?: "–",
                status = status,
                isNext = isNext,
                onClick = { viewModel.setStatus(prayer, viewModel.nextStatus(status)) },
            )
        }
        // Sunrise row (not toggleable)
        SunriseRow(
            time = times?.sunrise?.format(timeFormat) ?: "–",
        )
    }
}

@Composable
private fun PrayerRow(
    prayer: Prayer,
    time: String,
    status: PrayerStatus,
    isNext: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isNext) {
            CardDefaults.elevatedCardColors(containerColor = prayer.color().copy(alpha = 0.12f))
        } else {
            CardDefaults.elevatedCardColors()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(prayer.color()),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(prayer.displayName, style = MaterialTheme.typography.titleMedium)
                if (isNext) {
                    Text(
                        "Up next",
                        style = MaterialTheme.typography.labelSmall,
                        color = prayer.color(),
                    )
                }
            }
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(16.dp))
            Surface(
                shape = CircleShape,
                color = statusColor(status),
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = status.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SunriseRow(time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFD98A2B)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.today_sunrise),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun statusColor(status: PrayerStatus): Color = when (status) {
    PrayerStatus.PRAYED -> Color(0xFF2E7D56)
    PrayerStatus.DELAYED -> Color(0xFFD9A441)
    PrayerStatus.NOT_DID -> Color(0xFFB5542F)
    PrayerStatus.NOT_RECORDED -> MaterialTheme.colorScheme.surfaceVariant
}