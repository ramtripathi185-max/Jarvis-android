package com.example.core.service

import android.content.Context
import android.content.SharedPreferences

/**
 * PART 2B - State Manager for battery & service
 */
object JarvisServiceManager {

    private const val PREFS_NAME = "jarvis_state"
    private const val KEY_FOREGROUND_ACTIVE = "foreground_active"
    private const val KEY_BATTERY_OPTIMIZED = "battery_optimized"
    private const val KEY_TILE_ADDED = "tile_added"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setForegroundActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_FOREGROUND_ACTIVE, active).apply()
    }

    fun isForegroundActive(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_FOREGROUND_ACTIVE, false)
    }

    fun setBatteryOptimized(context: Context, done: Boolean) {
        prefs(context).edit().putBoolean(KEY_BATTERY_OPTIMIZED, done).apply()
    }

    fun isBatteryOptimized(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_BATTERY_OPTIMIZED, false)
    }
    
    fun getStatus(context: Context): String {
        return if (isForegroundActive(context)) "JARVIS Active" else "JARVIS Idle"
    }
}
