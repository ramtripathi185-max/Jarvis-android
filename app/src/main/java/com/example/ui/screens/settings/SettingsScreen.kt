package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.LanguageOption
import com.example.data.service.GeminiService
import com.example.ui.theme.CyberAccentNeon
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkNavy
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val currentLang by viewModel.selectedLanguage.collectAsState()
    val currentModel by viewModel.selectedModel.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val customTone by viewModel.customTone.collectAsState()
    val liveVoiceMode by viewModel.liveVoiceMode.collectAsState()
    val liveVoiceName by viewModel.liveVoiceName.collectAsState()

    val isApiKeyConfigured = viewModel.isApiKeyConfigured

    var customToneInput by remember(customTone) { mutableStateOf(customTone) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkNavy)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "JARVIS CONFIGURATION CORE",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "AI Model, Voice Persona & System Tuning",
            color = TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Gemini API Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_api_status_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardDark),
            border = BorderStroke(1.dp, if (isApiKeyConfigured) CyberAccentNeon.copy(alpha = 0.6f) else CyberWarning)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key",
                            tint = if (isApiKeyConfigured) CyberAccentNeon else CyberWarning,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini API Configuration",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isApiKeyConfigured) CyberAccentNeon.copy(alpha = 0.15f) else CyberWarning.copy(alpha = 0.15f),
                        border = BorderStroke(
                            0.8.dp,
                            if (isApiKeyConfigured) CyberAccentNeon.copy(alpha = 0.5f) else CyberWarning.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isApiKeyConfigured) "ACTIVE" else "KEY NEEDED",
                            color = if (isApiKeyConfigured) CyberAccentNeon else CyberWarning,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isApiKeyConfigured) {
                    Text(
                        text = "Gemini API key is configured securely via BuildConfig. Core service is online.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                } else {
                    Text(
                        text = "To enable Google Gemini AI:\n1. Open the Secrets panel in AI Studio UI.\n2. Add your GEMINI_API_KEY.\n3. The key is automatically injected securely into BuildConfig.",
                        color = CyberWarning,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Language Selection Card
        SettingsSectionHeader(icon = Icons.Default.Language, title = "CONVERSATION LANGUAGE")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardDark),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                LanguageOption.values().forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onLanguageChanged(lang) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLang == lang,
                            onClick = { viewModel.onLanguageChanged(lang) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CyberCyan,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = lang.displayName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (currentLang == lang) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Code: ${lang.tag}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Model Selection Card
        SettingsSectionHeader(icon = Icons.Default.Psychology, title = "GEMINI MODEL SELECTION")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardDark),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val models = listOf(
                    GeminiService.DEFAULT_MODEL to "Gemini 3.5 Flash (Fast, optimized for voice)",
                    GeminiService.PRO_MODEL to "Gemini 3.1 Pro (Deep reasoning, complex queries)"
                )
                models.forEach { (modelId, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onModelChanged(modelId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentModel == modelId,
                            onClick = { viewModel.onModelChanged(modelId) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CyberCyan,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = modelId,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (currentModel == modelId) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = desc,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3.5. Gemini Real-Time Live Voice Engine (Part 2A)
        SettingsSectionHeader(icon = Icons.Default.Speed, title = "GEMINI REAL-TIME LIVE VOICE (PART 2A)")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardDark),
            border = BorderStroke(1.dp, if (liveVoiceMode) CyberCyan.copy(alpha = 0.8f) else CyberBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time Streaming Voice",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Low-latency bidirectional streaming with instant barge-in / interruption support",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = liveVoiceMode,
                        onCheckedChange = { viewModel.onLiveVoiceModeChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDarkNavy,
                            checkedTrackColor = CyberCyan
                        )
                    )
                }

                if (liveVoiceMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Voice Character:",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val voiceCharacters = listOf("Puck", "Aoede", "Charon", "Fenrir", "Kore")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        voiceCharacters.forEach { charName ->
                            val isSelected = liveVoiceName == charName
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberCyan else CyberBorder
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.onLiveVoiceNameChanged(charName) }
                            ) {
                                Text(
                                    text = charName,
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Voice Engine / TTS Controls
        SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "FALLBACK SPEECH SYNTHESIS ENGINE")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardDark),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Auto-speak toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Speak Answers",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Voice vocalization of answers via TextToSpeech",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { viewModel.onAutoSpeakChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDarkNavy,
                            checkedTrackColor = CyberCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speech Rate Slider
                Text(
                    text = "Speech Rate: ${String.format("%.1fx", speechRate)}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = speechRate,
                    onValueChange = { viewModel.onSpeechRateChanged(it) },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = CyberBorder
                    )
                )

                // Pitch Slider
                Text(
                    text = "Vocal Pitch: ${String.format("%.1fx", pitch)}",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = pitch,
                    onValueChange = { viewModel.onPitchChanged(it) },
                    valueRange = 0.6f..1.6f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberAccentNeon,
                        activeTrackColor = CyberAccentNeon,
                        inactiveTrackColor = CyberBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Test Voice Button
                Button(
                    onClick = { viewModel.testTtsVoice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_test_voice_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan.copy(alpha = 0.2f),
                        contentColor = CyberCyan
                    ),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Test voice",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Test Vocal Delivery", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Personality System Prompt Customization
        SettingsSectionHeader(icon = Icons.Default.Tune, title = "PERSONALITY & SYSTEM INSTRUCTIONS")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardDark),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Add custom instructions to JARVIS persona (e.g. \"Address me as Boss\", \"Keep answers under 25 words\"):",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customToneInput,
                    onValueChange = {
                        customToneInput = it
                        viewModel.onCustomToneChanged(it)
                    },
                    placeholder = {
                        Text(
                            text = "e.g. Always address me respectfully as sir. Keep technical answers crisp.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_custom_tone_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberSurfaceDark,
                        unfocusedContainerColor = CyberSurfaceDark,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    ),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
