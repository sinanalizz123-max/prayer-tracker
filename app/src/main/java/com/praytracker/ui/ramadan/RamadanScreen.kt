package com.praytracker.ui.ramadan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.praytracker.PrayerTrackerApp
import com.praytracker.R
import com.praytracker.hijri.HijriDateUtil
import com.praytracker.ui.nav.ScreenScaffold
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RamadanScreen(
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as PrayerTrackerApp
    ScreenScaffold(title = stringResource(R.string.more_ramadan), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            var offset by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                offset = app.container.settingsRepository.snapshot().hijriOffsetDays
            }

            val today = LocalDate.now().plusDays(offset.toLong())

            // Compute next Ramadan info
            val info = remember(today) { computeRamadanInfo(today) }

            when (info.state) {
                RamadanState.IN_RAMADAN -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.ramadan_day_of, info.day, info.length),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.size(8.dp))
                            LinearProgressIndicator(
                                progress = { info.day.toFloat() / info.length },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.size(12.dp))
                            Text(
                                text = stringResource(R.string.ramadan_in_ramadan),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                RamadanState.SOON -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = info.nextStartDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.ramadan_not_in, info.nextStartDate.toString()),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                RamadanState.NONE -> {
                    Text("Ramadan date could not be computed.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private enum class RamadanState { IN_RAMADAN, SOON, NONE }

private data class RamadanInfo(
    val state: RamadanState,
    val day: Int = 0,
    val length: Int = 30,
    val nextStartDate: LocalDate = LocalDate.now(),
)

private fun computeRamadanInfo(today: LocalDate): RamadanInfo {
    val ramadan = HijriDateUtil.ramadanDay(today)
    if (ramadan != null) {
        return RamadanInfo(
            state = RamadanState.IN_RAMADAN,
            day = ramadan.first,
            length = ramadan.second,
        )
    }

    // Find next Gregorian date that is 1 Ramadan (scan up to 400 days ahead).
    var candidate = today.plusDays(1)
    var remaining = 400
    while (remaining-- > 0) {
        val h = HijriDateUtil.fromGregorian(candidate)
        if (h.isRamadan() && h.day == 1) {
            return RamadanInfo(
                state = RamadanState.SOON,
                nextStartDate = candidate,
            )
        }
        candidate = candidate.plusDays(1)
    }
    return RamadanInfo(state = RamadanState.NONE)
}