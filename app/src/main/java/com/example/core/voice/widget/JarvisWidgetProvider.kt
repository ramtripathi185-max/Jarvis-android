package com.example.core.voice.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.voice.entry.VoiceEntryActivity

/**
 * Android AppWidgetProvider delivering instant 1-tap voice entry from the Home Screen.
 */
class JarvisWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_MIC_CLICK) {
            val voiceIntent = Intent(context, VoiceEntryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(voiceIntent)
        }
    }

    companion object {
        const val ACTION_WIDGET_MIC_CLICK = "com.example.action.WIDGET_MIC_CLICK"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.jarvis_widget)

            // 1. Tapping the widget body opens MainActivity (full HUD)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpenApp = PendingIntent.getActivity(
                context,
                100,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_text_container, pendingOpenApp)
            views.setOnClickPendingIntent(R.id.widget_app_icon, pendingOpenApp)

            // 2. Tapping the microphone button launches the floating VoiceEntryActivity
            val voiceEntryIntent = Intent(context, VoiceEntryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingVoiceEntry = PendingIntent.getActivity(
                context,
                101,
                voiceEntryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mic_button, pendingVoiceEntry)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun notifyUpdateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, JarvisWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }
    }
}
