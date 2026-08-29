package com.praytracker.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import com.praytracker.prayer.Prayer
import java.time.Duration
import java.time.LocalDateTime

/**
 * Emits the current [LocalDateTime] every second. Used for live countdowns.
 */
@androidx.compose.runtime.Composable
fun currentTimeState(): androidx.compose.runtime.State<LocalDateTime> {
    return produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }
}

fun countdownText(now: LocalDateTime, target: LocalDateTime?): String? {
    if (target == null) return null
    val diff = Duration.between(now, target).seconds.coerceAtLeast(0)
    val hours = diff / 3600
    val minutes = (diff % 3600) / 60
    val seconds = diff % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun nextPrayer(now: LocalDateTime, times: com.praytracker.prayer.DailyPrayerTimes): Prayer? {
    var next: Prayer? = null
    var best: LocalDateTime? = null
    for (p in Prayer.ORDER) {
        val dt = times.localDateTime(p) ?: continue
        if (!dt.isBefore(now) && (best == null || dt.isBefore(best))) {
            best = dt
            next = p
        }
    }
    return next
}