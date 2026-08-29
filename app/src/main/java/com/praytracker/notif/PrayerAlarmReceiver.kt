package com.praytracker.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.praytracker.PrayerTrackerApp
import com.praytracker.data.settings.Settings
import com.praytracker.prayer.Prayer
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PrayerTrackerApp
        val notifier = PrayerNotifier(context)
        val container = app.container

        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER) ?: return
        val prayer = Prayer.entries.firstOrNull { it.name == prayerName } ?: return
        val timeText = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_TIME) ?: ""
        val isReminder = intent.action == PrayerAlarmScheduler.ACTION_REMINDER
        val date = runCatching { LocalDate.parse(intent.getStringExtra(PrayerAlarmScheduler.EXTRA_DATE)) }
            .getOrNull() ?: LocalDate.now()

        val result = goAsync()
        container.scope.launchSafely {
            try {
                val settings = container.settingsRepository.snapshot()
                if (!settings.notificationsEnabled || !settings.notifEnabledFor(prayer)) return@launchSafely
                if (isReminder) {
                    notifier.showReminder(
                        prayer,
                        settings.reminderMinutes,
                        timeText,
                    )
                } else {
                    notifier.showPrayerTime(
                        prayer,
                        timeText,
                        soundEnabled = settings.notifSoundEnabled,
                        vibrateEnabled = settings.notifVibrateEnabled,
                    )
                }
                container.prayerAlarmScheduler.scheduleNextOccurrence(prayer, date, isReminder)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                result.finish()
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchSafely(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    kotlinx.coroutines.launch(block = block)
}