package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE) }
    
    // Check if key is coming from BuildConfig or SharedPreferences
    val buildConfigKey = try { BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String } catch (e: Exception) { null }
    val savedKey = sharedPreferences.getString("gemini_api_key", "") ?: ""
    val activeKey = if (!buildConfigKey.isNull_or_Empty()) buildConfigKey else savedKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "JARVIS CONFIGURATION CORE",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gemini API Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (activeKey.isNotEmpty()) {
                    Text(
                        text = "STATUS: KEY ACTIVE & CONFIGURED",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                } else {
                    Text(
                        text = "STATUS: KEY NEEDED",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

private fun String?.isNull_or_Empty(): Boolean = this == null || this.isEmpty()
