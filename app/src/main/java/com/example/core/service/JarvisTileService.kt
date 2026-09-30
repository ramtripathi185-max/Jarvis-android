package com.example.core.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * PART 2B - Quick Settings Tile
 * Swipe down se JARVIS on/off
 */
@RequiresApi(Build.VERSION_CODES.N)
class JarvisTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val isActive = isJarvisRunning()
        if (isActive) {
            JarvisForegroundService.stop(this)
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            JarvisForegroundService.start(this)
            qsTile.state = Tile.STATE_ACTIVE
        }
        qsTile.updateTile()
        // Explicit voice entry after toggle on
        if (!isActive) {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                putExtra("extra_voice_entry", true)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            intent?.let { startActivityAndCollapse(it) }
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (isJarvisRunning()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "JARVIS"
        tile.subtitle = if (tile.state == Tile.STATE_ACTIVE) "Active" else "Tap to start"
        tile.updateTile()
    }

    private fun isJarvisRunning(): Boolean {
        // Persistent state check - reads from SharedPrefs
        val prefs = getSharedPreferences("jarvis_state", MODE_PRIVATE)
        return prefs.getBoolean("foreground_active", false)
    }
}
