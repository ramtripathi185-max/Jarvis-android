package com.example.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.jarvis.R

class JarvisForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "jarvis_foreground_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.core.service.ACTION_START"
        const val ACTION_STOP = "com.example.core.service.ACTION_STOP"
        const val ACTION_VOICE_ENTRY = "com.example.core.service.ACTION_VOICE_ENTRY"
        fun start(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
        fun stop(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
    override fun onCreate() { super.onCreate(); createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_STOP -> { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            ACTION_VOICE_ENTRY -> handleVoiceEntry()
            else -> startForeground(NOTIFICATION_ID, buildNotif())
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun handleVoiceEntry() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply { putExtra("extra_voice_entry", true); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        launchIntent?.let { startActivity(it) }
    }
    private fun buildNotif(): Notification {
        val stopIntent = Intent(this, JarvisForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val voiceIntent = Intent(this, JarvisForegroundService::class.java).apply { action = ACTION_VOICE_ENTRY }
        val voicePI = PendingIntent.getService(this, 2, voiceIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val mainIntent = packageManager.getLaunchIntentForPackage(packageName)
        val mainPI = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS is Active")
            .setContentText("Tap to talk - Battery optimized")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(mainPI)
            .addAction(0, "Talk", voicePI)
            .addAction(0, "Stop", stopPI)
            .build()
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "JARVIS Service", NotificationManager.IMPORTANCE_LOW).apply { description = "Persistent controls"; setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
}
