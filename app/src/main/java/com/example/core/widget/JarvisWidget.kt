package com.example.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.jarvis.R
import com.example.core.service.JarvisForegroundService

/**
 * PART 2B - Home-screen widget
 * Battery-conscious widget with voice entry
 */
class JarvisWidget : AppWidgetProvider {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_jarvis)

            // Tap widget -> explicit voice entry (no always-on mic)
            val voiceIntent = Intent(context, JarvisForegroundService::class.java).apply {
                action = JarvisForegroundService.ACTION_VOICE_ENTRY
            }
            val voicePendingIntent = PendingIntent.getService(
                context, 0, voiceIntent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, voicePendingIntent)
            views.setOnClickPendingIntent(R.id.btn_talk, voicePendingIntent)

            // Start/Stop intent
            val startIntent = Intent(context, JarvisForegroundService::class.java).apply {
                action = JarvisForegroundService.ACTION_START
            }
            val startPI = PendingIntent.getService(context, 1, startIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.btn_power, startPI)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
