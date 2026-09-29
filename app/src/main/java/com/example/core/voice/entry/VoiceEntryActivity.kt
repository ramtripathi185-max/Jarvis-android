package com.example.core.voice.entry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.JarvisApplication
import com.example.MainActivity
import com.example.core.model.AssistantState
import com.example.ui.components.JarvisArcReactor
import com.example.ui.theme.CyberAccentNeon
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkNavy
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Floating / Translucent Quick Voice Entry Activity.
 * Provides frictionless system-wide access from Quick Settings Tile, Widget, or Assist intent.
 */
class VoiceEntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as JarvisApplication
        val engine = app.assistantEngine

        setContent {
            MyApplicationTheme {
                val state by engine.state.collectAsState()

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        engine.startVoiceListening { err ->
                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Microphone access required for voice command.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                LaunchedEffect(Unit) {
                    val hasMic = ContextCompat.checkSelfPermission(
                        this@VoiceEntryActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasMic) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        engine.startVoiceListening { err ->
                            Toast.makeText(this@VoiceEntryActivity, err, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                VoiceEntryDialogContent(
                    state = state,
                    onStopOrMicClick = {
                        if (state is AssistantState.Listening || state is AssistantState.Speaking) {
                            engine.cancelActiveOperation()
                        } else {
                            engine.startVoiceListening { err ->
                                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onOpenFullApp = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDismiss = {
                        engine.cancelActiveOperation()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceEntryDialogContent(
    state: AssistantState,
    onStopOrMicClick: () -> Unit,
    onOpenFullApp: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {} // Prevent dismiss when tapping inside dialog
                .testTag("voice_entry_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CyberDarkNavy),
            border = BorderStroke(1.5.dp, CyberCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyberAccentNeon)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JARVIS VOICE LINK",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Compact Arc Reactor
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    JarvisArcReactor(
                        state = state,
                        size = 180.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyberSurfaceDark,
                    border = BorderStroke(
                        1.dp,
                        when (state) {
                            is AssistantState.Listening -> CyberAccentNeon
                            is AssistantState.Thinking -> CyberWarning
                            is AssistantState.Speaking -> CyberCyan
                            is AssistantState.Error -> CyberError
                            else -> CyberBorder
                        }
                    )
                ) {
                    val statusText = when (state) {
                        is AssistantState.Idle -> "READY • LISTENING"
                        is AssistantState.Listening -> "LISTENING..."
                        is AssistantState.Thinking -> "THINKING..."
                        is AssistantState.Speaking -> "SPEAKING..."
                        is AssistantState.Error -> "ERROR DETECTED"
                    }
                    Text(
                        text = statusText,
                        color = when (state) {
                            is AssistantState.Listening -> CyberAccentNeon
                            is AssistantState.Thinking -> CyberWarning
                            is AssistantState.Speaking -> CyberCyan
                            is AssistantState.Error -> CyberError
                            else -> TextSecondary
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dialogue / Subtitle Text
                val promptText = when (val s = state) {
                    is AssistantState.Listening -> if (s.partialText.isNotBlank()) "\"${s.partialText}\"" else "Speak your request in English, Hindi or Hinglish..."
                    is AssistantState.Thinking -> if (s.query.isNotBlank()) "\"${s.query}\"" else "Synthesizing answer..."
                    is AssistantState.Speaking -> s.speechText
                    is AssistantState.Error -> s.message
                    is AssistantState.Idle -> "Tap microphone to speak to JARVIS"
                }

                Text(
                    text = promptText,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onOpenFullApp,
                        border = BorderStroke(1.dp, CyberBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                    ) {
                        Icon(imageVector = Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Open HUD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStopOrMicClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state is AssistantState.Listening || state is AssistantState.Speaking) CyberError else CyberCyan
                        )
                    ) {
                        val isBusy = state is AssistantState.Listening || state is AssistantState.Speaking
                        Icon(
                            imageVector = if (isBusy) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isBusy) Color.White else CyberDarkNavy,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBusy) "Stop" else "Speak",
                            color = if (isBusy) Color.White else CyberDarkNavy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
