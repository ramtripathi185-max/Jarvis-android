package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AssistantState
import com.example.ui.theme.CyberAccentNeon
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CyberHeader(
    assistantState: AssistantState,
    isOnline: Boolean,
    currentModel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = CyberSurfaceDark.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // JARVIS Brand & Version Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !isOnline -> CyberError
                                assistantState is AssistantState.Listening -> CyberAccentNeon
                                assistantState is AssistantState.Thinking -> CyberWarning
                                assistantState is AssistantState.Speaking -> CyberCyan
                                else -> CyberCyan
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "MARK I • SECURE AI CORE",
                        color = CyberCyan.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Model & Link Status Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Model Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberCardDark,
                    border = BorderStroke(0.8.dp, CyberBorder)
                ) {
                    Text(
                        text = currentModel.replace("gemini-", "").take(10),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Online/Offline Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isOnline) CyberAccentNeon.copy(alpha = 0.15f) else CyberError.copy(alpha = 0.15f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isOnline) CyberAccentNeon.copy(alpha = 0.5f) else CyberError.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (isOnline) "SYNCED" else "OFFLINE",
                        color = if (isOnline) CyberAccentNeon else CyberError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
