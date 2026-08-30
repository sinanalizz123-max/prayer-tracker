package com.praytracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praytracker.ui.MainViewModel
import com.praytracker.ui.theme.EmeraldGreen
import com.praytracker.util.HijriHelper
import com.praytracker.util.PrayerCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsChanged by viewModel.settings.settingsChanged.collectAsState()
    val hijriCurrent by viewModel.hijriDate.collectAsState()
    val settings = viewModel.settings

    // Reading settingsChanged keeps this sheet subscribed to settings writes so the
    // getter-backed values below (adjustment, Arabic numerals toggle) refresh live.
    remember(settingsChanged) { }

    val currentHijriMonth = hijriCurrent.month
    val currentHijriYear = hijriCurrent.year
    val hijriAdjustment = settings.hijriAdjustment
    val useArabic = settings.useArabicNumerals

    var viewedMonth by remember(currentHijriMonth) { mutableIntStateOf(currentHijriMonth) }
    var viewedYear by remember(currentHijriYear) { mutableIntStateOf(currentHijriYear) }

    val todayGregorian = LocalDate.now()
    val monthDays = remember(viewedYear, viewedMonth, hijriAdjustment) {
        HijriHelper.getHijriMonthDays(viewedYear, viewedMonth, hijriAdjustment, todayGregorian)
    }

    var selectedDay by remember(viewedYear, viewedMonth, hijriAdjustment) {
        mutableStateOf(monthDays.firstOrNull { it.isToday } ?: monthDays.firstOrNull())
    }

    val firstDayWeekdayIndex = if (monthDays.isNotEmpty()) {
        // DayOfWeek 1 (Mon) .. 7 (Sun) -> convert to 0 (Sun) .. 6 (Sat)
        val dow = monthDays.first().dayOfWeek.value
        dow % 7
    } else 0

    val monthNameEn = HijriHelper.MONTHS_EN.getOrElse(viewedMonth - 1) { "Month $viewedMonth" }
    val monthNameAr = HijriHelper.MONTHS_AR.getOrElse(viewedMonth - 1) { "" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("hijri_calendar_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Close
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hijri Calendar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Umm al-Qura Islamic lunar dates & moon adjustments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Month Navigator Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (viewedMonth == 1) {
                                    viewedMonth = 12
                                    viewedYear -= 1
                                } else {
                                    viewedMonth -= 1
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val yearDisplay = if (useArabic) HijriHelper.toArabicNumbers(viewedYear.toString()) else viewedYear.toString()
                            Text(
                                text = "$monthNameEn $yearDisplay AH",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$monthNameAr (${viewedMonth}/12)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                if (viewedMonth == 12) {
                                    viewedMonth = 1
                                    viewedYear += 1
                                } else {
                                    viewedMonth += 1
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }
            }

            // Moon Sighting / Edit Calendar Quick Adjuster Section
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Nightlight,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Moon Sighting / Date Edit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (viewedMonth != currentHijriMonth || viewedYear != currentHijriYear) {
                                TextButton(
                                    onClick = {
                                        viewedMonth = currentHijriMonth
                                        viewedYear = currentHijriYear
                                    }
                                ) {
                                    Text("Jump to Current", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "If the new crescent moon was sighted earlier or later in your region, adjust the calendar by ±1 or ±2 days:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Adjustment Chips [-2, -1, 0, +1, +2]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(-2, -1, 0, 1, 2).forEach { offset ->
                                val isSelected = (hijriAdjustment == offset)
                                val label = when {
                                    offset == 0 -> "0 (Default)"
                                    offset > 0 -> "+$offset d"
                                    else -> "$offset d"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setHijriAdjustment(offset) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Calendar Weekday Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HijriHelper.WEEKDAYS_EN.forEachIndexed { index, dayName ->
                        val isFriday = (index == 5)
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isFriday) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isFriday) EmeraldGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Calendar Days Grid
            item {
                val totalCells = firstDayWeekdayIndex + monthDays.size
                val rows = (totalCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayIndex = cellIndex - firstDayWeekdayIndex

                                if (dayIndex in monthDays.indices) {
                                    val dayInfo = monthDays[dayIndex]
                                    val isSelected = (selectedDay?.hijriDay == dayInfo.hijriDay)
                                    val dayDisplay = if (useArabic) HijriHelper.toArabicNumbers(dayInfo.hijriDay.toString()) else dayInfo.hijriDay.toString()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> EmeraldGreen.copy(alpha = 0.2f)
                                                    dayInfo.isToday -> EmeraldGreen.copy(alpha = 0.1f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else if (dayInfo.isToday) 1.dp else 0.dp,
                                                color = if (isSelected) EmeraldGreen else if (dayInfo.isToday) EmeraldGreen.copy(alpha = 0.6f) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedDay = dayInfo }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayDisplay,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (dayInfo.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (dayInfo.isToday) EmeraldGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${dayInfo.gregorianDate.dayOfMonth} ${dayInfo.gregorianDate.month.name.take(3)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            if (dayInfo.eventName != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(EmeraldGreen)
                                                )
                                            } else if (dayInfo.isWhiteDay) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Empty blank cell before start of month or after end
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Detail Card (Shows full date, events, and calculated prayer schedule)
            item {
                selectedDay?.let { day ->
                    val dayFormattedGreg = day.gregorianDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
                    val selectedSchedule = PrayerCalculator.calculateSchedule(
                        lat = settings.latitude,
                        lon = settings.longitude,
                        timezoneId = settings.timezoneId,
                        localDate = day.gregorianDate,
                        settings = settings
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val hijriDayStr = if (useArabic) HijriHelper.toArabicNumbers(day.hijriDay.toString()) else day.hijriDay.toString()
                            val hijriYearStr = if (useArabic) HijriHelper.toArabicNumbers(day.hijriYear.toString()) else day.hijriYear.toString()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$hijriDayStr $monthNameEn $hijriYearStr AH",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = dayFormattedGreg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                if (day.isToday) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "TODAY",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Event badge
                            if (day.eventName != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = day.eventName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                    }
                                }
                            } else if (day.isWhiteDay) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "White Day (Ayyam al-Beed Sunnah Fasting)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Day's Prayer Schedule Grid
                            Text(
                                text = "PRAYER TIMES FOR THIS DATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                selectedSchedule.list.forEach { item ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = item.formattedTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Toggle for Arabic vs Normal Numerals
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Arabic Numerals (١، ٢، ٣)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (useArabic) "Arabic numerals enabled" else "Standard numbers (1, 2, 3) active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        Switch(
                            checked = useArabic,
                            onCheckedChange = { settings.useArabicNumerals = it }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
