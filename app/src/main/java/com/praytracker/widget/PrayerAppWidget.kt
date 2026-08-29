package com.praytracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.praytracker.PrayerTrackerApp
import com.praytracker.prayer.Prayer
import com.praytracker.ui.MainActivity
import com.praytracker.util.nextPrayer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerAppWidget()
}

class PrayerAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as PrayerTrackerApp).container
        val settings = container.settingsRepository.snapshot()
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val times = container.prayerTimesRepository.compute(today, settings)

        val nextPrayerName: String
        val nextTime: String
        val nextPrayer = nextPrayer(now, times)
        if (nextPrayer != null) {
            val dt = times.localDateTime(nextPrayer)
            nextPrayerName = nextPrayer.displayName
            nextTime = dt?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "–"
        } else {
            val tomorrow = container.prayerTimesRepository.compute(today.plusDays(1), settings)
            val first = Prayer.ORDER.firstNotNullOfOrNull { p ->
                tomorrow.localDateTime(p)?.let { Pair(p.displayName, it.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))) }
            }
            nextPrayerName = first?.first ?: "Fajr"
            nextTime = first?.second ?: "–"
        }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(GlanceTheme.colors.primaryContainer))
                        .cornerRadius(20.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Next prayer",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = GlanceTheme.colors.onPrimaryContainer,
                        ),
                    )
                    Spacer(GlanceModifier.size(4.dp))
                    Text(
                        text = nextPrayerName.uppercase(),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary,
                        ),
                    )
                    Spacer(GlanceModifier.size(4.dp))
                    Text(
                        text = nextTime,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    }
}