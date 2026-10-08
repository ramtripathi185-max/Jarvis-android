package com.example.ui.navigation

import androidx.compose.runtime.Composable
import com.example.ui.screens.SettingsScreen

@Composable
fun JarvisNavHost() {
    SettingsScreen()
}

@Composable
fun JarvisNavScreen() {
    JarvisNavHost()
}
