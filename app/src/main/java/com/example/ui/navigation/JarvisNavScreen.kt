package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDarkNavy
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.TextMuted

enum class JarvisTab(val title: String, val icon: ImageVector, val tag: String) {
    HUD("HUD", Icons.Default.RadioButtonChecked, "nav_tab_hud"),
    CONVERSATION("Dialogue", Icons.AutoMirrored.Filled.Chat, "nav_tab_conversation"),
    PERMISSIONS("Systems", Icons.Default.Security, "nav_tab_permissions"),
    SETTINGS("Config", Icons.Default.Settings, "nav_tab_settings")
}

@Composable
fun JarvisNavHost(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(JarvisTab.HUD) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = selectedTab != JarvisTab.HUD) {
        selectedTab = JarvisTab.HUD
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("jarvis_bottom_navigation")
            ) {
                JarvisTab.values().forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = CyberBorder.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberDarkNavy)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                JarvisTab.HUD -> Text(text = "HUD Screen", color = CyberCyan)
                JarvisTab.CONVERSATION -> Text(text = "Conversation Screen", color = CyberCyan)
                JarvisTab.PERMISSIONS -> Text(text = "Permissions Screen", color = CyberCyan)
                JarvisTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
