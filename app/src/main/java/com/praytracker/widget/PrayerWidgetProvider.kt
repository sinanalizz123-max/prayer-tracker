package com.praytracker.widget

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.praytracker.MainActivity
import com.praytracker.R
import com.praytracker.data.SettingsManager
import com.praytracker.util.PrayerCalculator
import java.time.ZonedDateTime

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val settings = SettingsManager(context)
        val now = ZonedDateTime.now()

        val nextPrayerInfo = try {
            PrayerCalculator.getNextPrayer(
                lat = settings.latitude,
                lon = settings.longitude,
                timezoneId = settings.timezoneId,
                now = now,
                settings = settings
            )
        } catch (e: Exception) {
            null
        }

        val views = RemoteViews(context.packageName, R.layout.prayer_widget)
        views.setTextViewText(R.id.widget_location, settings.locationName)

        if (nextPrayerInfo != null) {
            views.setTextViewText(R.id.widget_prayer_title, "Next: ${nextPrayerInfo.name}")
            views.setTextViewText(R.id.widget_prayer_time, nextPrayerInfo.formattedTime)
            
            val mins = nextPrayerInfo.countdownMinutes
            val countdownText = if (mins >= 60) {
                "- in ${mins / 60}h ${mins % 60}m"
            } else {
                "- in ${mins}m"
            }
            views.setTextViewText(R.id.widget_countdown, countdownText)
        } else {
            views.setTextViewText(R.id.widget_prayer_title, "Prayer Times")
            views.setTextViewText(R.id.widget_prayer_time, "--:--")
            views.setTextViewText(R.id.widget_countdown, "Setup location")
        }

        // Tap to open main activity
        val intent = Intent(context, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
        views.setOnClickPendingIntent(R.id.widget_prayer_title, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_prayer_time, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
