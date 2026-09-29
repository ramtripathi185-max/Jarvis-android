package com.example.ui.screens.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AssistantState
import com.example.ui.components.ActionConfirmationCard
import com.example.ui.components.CyberHeader
import com.example.ui.components.JarvisArcReactor
import com.example.ui.theme.CyberAccentNeon
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkNavy
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun HudScreen(
    viewModel: JarvisViewModel,
    onRequestPermission: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val currentModel by viewModel.selectedModel.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val isLiveVoice by viewModel.liveVoiceMode.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkNavy)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Futuristic Telemetry Top Bar
        CyberHeader(
            assistantState = assistantState,
            isOnline = permissionState.isNetworkConnected,
            currentModel = currentModel,
            isLiveVoice = isLiveVoice
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Center Stage: Animated Circular Voice Reactor
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .testTag("hud_arc_reactor_container"),
            contentAlignment = Alignment.Center
        ) {
            JarvisArcReactor(
                state = assistantState,
                size = 280.dp
            )
        }

        // Assistant State Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CyberSurfaceDark,
            border = BorderStroke(
                1.dp,
                when (assistantState) {
                    is AssistantState.Listening -> CyberAccentNeon
                    is AssistantState.Thinking -> CyberWarning
                    is AssistantState.Speaking -> CyberCyan
                    is AssistantState.Error -> CyberError
                    else -> CyberBorder
                }
            )
        ) {
            Text(
                text = when (val s = assistantState) {
                    is AssistantState.Idle -> "SYSTEM READY • STANDBY"
                    is AssistantState.Listening -> if (s.partialText.isNotBlank()) "HEARING: \"${s.partialText}\"" else "LISTENING TO VOCAL INPUT..."
                    is AssistantState.Thinking -> "PROCESSING WITH GEMINI..."
                    is AssistantState.Speaking -> "TRANSMITTING VOCAL SYNTHESIS..."
                    is AssistantState.Error -> "SYSTEM ANOMALY DETECTED"
                },
                color = when (assistantState) {
                    is AssistantState.Listening -> CyberAccentNeon
                    is AssistantState.Thinking -> CyberWarning
                    is AssistantState.Speaking -> CyberCyan
                    is AssistantState.Error -> CyberError
                    else -> TextSecondary
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Subtitle Display Box
        val subtitleText = when (val s = assistantState) {
            is AssistantState.Listening -> if (s.partialText.isNotBlank()) s.partialText else "Listening for speech... (English, Hindi, or Hinglish)"
            is AssistantState.Thinking -> if (s.query.isNotBlank()) "\"${s.query}\"" else "Synthesizing answer..."
            is AssistantState.Speaking -> s.speechText
            is AssistantState.Error -> s.message
            is AssistantState.Idle -> "Tap microphone or say a query to activate JARVIS core."
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
            color = CyberSurfaceDark.copy(alpha = 0.6f),
            border = BorderStroke(0.8.dp, CyberBorder)
        ) {
            Text(
                text = subtitleText,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp)
            )
        }

        // Pending Action Confirmation Card (Safety Protocol)
        AnimatedVisibility(
            visible = pendingAction != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            pendingAction?.let { action ->
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    ActionConfirmationCard(
                        pendingAction = action,
                        onConfirm = { viewModel.onResolvePendingAction(approved = true) },
                        onCancel = { viewModel.onResolvePendingAction(approved = false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Center Voice Activation Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional Abort button if busy
            if (assistantState !is AssistantState.Idle) {
                IconButton(
                    onClick = { viewModel.onCancelOperation() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyberError.copy(alpha = 0.2f))
                        .border(1.dp, CyberError, CircleShape)
                        .testTag("hud_abort_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = CyberError,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
            }

            // Primary Mic Button
            val isListening = assistantState is AssistantState.Listening
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isListening) {
                                listOf(CyberAccentNeon, CyberAccentNeon.copy(alpha = 0.4f))
                            } else {
                                listOf(CyberCyan, CyberCyan.copy(alpha = 0.3f))
                            }
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = if (isListening) CyberAccentNeon else CyberCyan,
                        shape = CircleShape
                    )
                    .clickable {
                        if (!permissionState.hasAudioPermission) {
                            onRequestPermission()
                        } else {
                            viewModel.onVoiceMicClick(onError = { err -> onShowSnackbar(err) })
                        }
                    }
                    .testTag("hud_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                    tint = CyberDarkNavy,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Suggestion Chips Header
        Text(
            text = "QUICK PROTOCOLS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Action Chips
        val suggestions = listOf(
            "What time is it?" to "action_time",
            "Check battery level" to "action_battery",
            "System diagnostics" to "action_diagnostics",
            "Explain quantum computing in Hinglish" to "prompt_hinglish",
            "Aaj ka mausam kaisa hai?" to "prompt_weather",
            "Purge cache" to "action_purge"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            suggestions.forEach { (query, tag) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCardDark,
                    border = BorderStroke(1.dp, CyberBorder),
                    modifier = Modifier
                        .clickable { viewModel.onSendTextMessage(query) }
                        .testTag("chip_$tag")
                ) {
                    Text(
                        text = query,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
