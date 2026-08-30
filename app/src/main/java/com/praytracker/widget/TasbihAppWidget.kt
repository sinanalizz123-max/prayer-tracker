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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praytracker.PrayerTrackerApp
import com.praytracker.data.db.TasbihEntity
import com.praytracker.ui.MainActivity

class TasbihWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasbihAppWidget()
}

class TasbihAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasbih = (context.applicationContext as PrayerTrackerApp)
            .container.tasbihRepository.getPrimaryOrDefault() ?: TasbihEntity(
                id = 1L,
                arabic = "سُبْحَانَ اللّٰه",
                translation = "Glory be to Allah",
                target = 33,
            )

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(20.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(GlanceModifier.size(10.dp))
                    Text(
                        text = tasbih.arabic,
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onPrimaryContainer,
                        ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.size(6.dp))
                    Text(
                        text = "${tasbih.count} / ${tasbih.target}",
                        style = TextStyle(
                            fontSize = 18.sp,
                            color = GlanceTheme.colors.onPrimaryContainer,
                        ),
                    )
                    Spacer(GlanceModifier.size(6.dp))
                    Text(
                        text = "Open app to count",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    }
}