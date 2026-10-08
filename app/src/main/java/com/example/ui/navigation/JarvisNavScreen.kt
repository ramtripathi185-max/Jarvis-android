package com.example.ui.navigatio
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HudScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen

sealed class JarvisScreen(val route: String) {
    object Hud : JarvisScreen("hud")
    object Conversation : JarvisScreen("conversation")
    object Settings : JarvisScreen("settings")
    object Permissions : JarvisScreen("permissions")
}

@Composable
fun JarvisNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = JarvisScreen.Settings.route
    ) {
        composable(JarvisScreen.Hud.route) {
            HudScreen()
        }
        composable(JarvisScreen.Conversation.route) {
            ConversationScreen()
        }
        composable(JarvisScreen.Settings.route) {
            SettingsScreen()
        }
        composable(JarvisScreen.Permissions.route) {
            PermissionsScreen()
        }
    }
}

@Composable
fun JarvisNavScreen() {
    JarvisNavHost()
}
