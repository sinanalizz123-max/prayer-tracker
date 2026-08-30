package com.praytracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.praytracker.R
import com.praytracker.data.AppDatabase
import com.praytracker.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class TasbihWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == "com.praytracker.action.WIDGET_INCREMENT" || action == "com.praytracker.action.WIDGET_RESET") {
            val pendingResult = goAsync()
            val db = AppDatabase.getDatabase(context.applicationContext, CoroutineScope(SupervisorJob()))
            val dao = db.tasbihDao()
            val settings = SettingsManager(context)
            val selectedId = settings.selectedTasbihId

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Try to fetch selected item, otherwise fallback to first
                    val item = dao.getTasbihItemById(selectedId) ?: dao.getAllTasbihItems().firstOrNull()?.firstOrNull()
                    if (item != null) {
                        if (action == "com.praytracker.action.WIDGET_INCREMENT") {
                            dao.updateTasbihItem(item.copy(currentCount = item.currentCount + 1))
                        } else {
                            dao.updateTasbihItem(item.copy(currentCount = 0))
                        }

                        // Trigger visual update for all active widgets of this type
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val thisAppWidget = ComponentName(context, TasbihWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
                        onUpdate(context, appWidgetManager, appWidgetIds)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val settings = SettingsManager(context)
        val selectedId = settings.selectedTasbihId

        val db = AppDatabase.getDatabase(context.applicationContext, CoroutineScope(SupervisorJob()))
        val dao = db.tasbihDao()

        CoroutineScope(Dispatchers.IO).launch {
            val item = dao.getTasbihItemById(selectedId) ?: dao.getAllTasbihItems().firstOrNull()?.firstOrNull()
            
            val views = RemoteViews(context.packageName, R.layout.tasbih_widget)
            if (item != null) {
                views.setTextViewText(R.id.widget_dhikr_title, item.englishText)
                views.setTextViewText(R.id.widget_tasbih_count, item.currentCount.toString())
            } else {
                views.setTextViewText(R.id.widget_dhikr_title, "SubhanAllah")
                views.setTextViewText(R.id.widget_tasbih_count, "0")
            }

            // Setup button click action pending intents
            val incIntent = Intent(context, TasbihWidgetProvider::class.java).apply {
                action = "com.praytracker.action.WIDGET_INCREMENT"
            }
            val resetIntent = Intent(context, TasbihWidgetProvider::class.java).apply {
                action = "com.praytracker.action.WIDGET_RESET"
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val incPending = PendingIntent.getBroadcast(context, 201, incIntent, flags)
            val resetPending = PendingIntent.getBroadcast(context, 202, resetIntent, flags)

            views.setOnClickPendingIntent(R.id.widget_btn_increment, incPending)
            views.setOnClickPendingIntent(R.id.widget_btn_reset, resetPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
