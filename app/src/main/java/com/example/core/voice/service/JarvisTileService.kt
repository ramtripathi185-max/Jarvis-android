package com.example.core.voice.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.example.JarvisApplication
import com.example.core.model.AssistantState
import com.example.core.voice.entry.VoiceEntryActivity

/**
 * Android Quick Settings Tile Service.
 * Allows activating JARVIS voice interaction directly from the system notification shade.
 */
class JarvisTileService : TileService() {

    private val tag = "JarvisTileService"

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        Log.d(tag, "Quick Settings tile clicked. Launching VoiceEntryActivity.")

        val voiceIntent = Intent(this, VoiceEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires PendingIntent for TileService.startActivityAndCollapse
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                200,
                voiceIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(voiceIntent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        try {
            val app = applicationContext as? JarvisApplication
            val state = app?.assistantEngine?.state?.value

            when (state) {
                is AssistantState.Listening, is AssistantState.Speaking, is AssistantState.Thinking -> {
                    tile.state = Tile.STATE_ACTIVE
                    tile.subtitle = "Active Link"
                }
                else -> {
                    tile.state = Tile.STATE_INACTIVE
                    tile.subtitle = "Tap to speak"
                }
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.w(tag, "Error updating QS tile state", e)
        }
    }
}
