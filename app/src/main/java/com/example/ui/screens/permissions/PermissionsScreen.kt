package com.example.ui.screens.permissions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun PermissionsScreen(
    viewModel: JarvisViewModel,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionState by viewModel.permissionState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkNavy)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SYSTEM PERMISSIONS & LINK",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Hardware Access & Security Status",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { viewModel.refreshPermissions() },
                modifier = Modifier.testTag("permissions_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Status",
                    tint = CyberCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overall Health Card
        val isNominal = permissionState.isFullyOperational
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CyberSurfaceDark,
            border = BorderStroke(1.2.dp, if (isNominal) CyberAccentNeon else CyberWarning),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isNominal) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = "Status",
                    tint = if (isNominal) CyberAccentNeon else CyberWarning,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isNominal) "ALL SYSTEMS OPERATIONAL" else "ATTENTION REQUIRED",
                        color = if (isNominal) CyberAccentNeon else CyberWarning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isNominal) "Microphone, Speech Recognition and Network Link are fully online."
                        else "One or more foundational services require configuration.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ACTIVE PERMISSION CHANNELS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Microphone Access
        PermissionItemCard(
            title = "Microphone Access",
            description = "Allows JARVIS to listen to spoken queries in real-time.",
            icon = Icons.Default.Mic,
            isGranted = permissionState.hasAudioPermission,
            actionLabel = if (!permissionState.hasAudioPermission) "Grant Access" else null,
            onAction = onRequestPermission,
            testTag = "permission_item_microphone"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Speech Recognition Service
        PermissionItemCard(
            title = "Speech Recognition Engine",
            description = "Android system speech-to-text service availability.",
            icon = Icons.Default.RecordVoiceOver,
            isGranted = permissionState.isSpeechRecognitionAvailable,
            testTag = "permission_item_speech_engine"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Network Link
        PermissionItemCard(
            title = "Gemini Cloud Uplink",
            description = "High-speed secure connection to Google Gemini API servers.",
            icon = Icons.Default.Wifi,
            isGranted = permissionState.isNetworkConnected,
            testTag = "permission_item_network"
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Future Permissions Roadmap
        Text(
            text = "MODULAR CAPABILITIES ROADMAP (PARTS 2-5)",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(10.dp))

        FutureModuleCard(
            title = "Telephony & Contacts Module",
            description = "Part 2 will add direct voice calling and contact lookup with safety authorization.",
            icon = Icons.Default.Phone,
            partBadge = "PART 2"
        )

        Spacer(modifier = Modifier.height(8.dp))

        FutureModuleCard(
            title = "Communication & Messaging",
            description = "Part 3 will introduce WhatsApp messaging and interactive notification dispatch.",
            icon = Icons.Default.Notifications,
            partBadge = "PART 3"
        )

        Spacer(modifier = Modifier.height(8.dp))

        FutureModuleCard(
            title = "Media & Productivity Core",
            description = "Parts 4 & 5 will integrate YouTube audio/video controls and calendar reminders.",
            icon = Icons.Default.ContactPhone,
            partBadge = "PARTS 4-5"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardDark),
        border = BorderStroke(1.dp, CyberBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) CyberCyan else CyberWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGranted) CyberAccentNeon.copy(alpha = 0.15f) else CyberError.copy(alpha = 0.15f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isGranted) CyberAccentNeon.copy(alpha = 0.5f) else CyberError.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (isGranted) "AUTHORIZED" else "NOT GRANTED",
                        color = if (isGranted) CyberAccentNeon else CyberError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberDarkNavy),
                    modifier = Modifier.testTag("${testTag}_action")
                ) {
                    Text(text = actionLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FutureModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    partBadge: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark.copy(alpha = 0.6f)),
        border = BorderStroke(0.6.dp, CyberBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CyberCardDark,
                        border = BorderStroke(0.5.dp, CyberBorder)
                    ) {
                        Text(
                            text = partBadge,
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
