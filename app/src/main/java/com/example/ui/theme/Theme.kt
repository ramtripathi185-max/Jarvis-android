package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val JarvisDarkColorScheme = darkColorScheme(
  primary = JarvisPrimary,
  onPrimary = JarvisOnPrimary,
  primaryContainer = JarvisPrimaryContainer,
  onPrimaryContainer = JarvisOnPrimaryContainer,
  secondary = JarvisSecondary,
  onSecondary = JarvisOnSecondary,
  secondaryContainer = JarvisSecondaryContainer,
  onSecondaryContainer = JarvisOnSecondaryContainer,
  tertiary = JarvisTertiary,
  onTertiary = JarvisOnTertiary,
  background = JarvisBackground,
  onBackground = JarvisOnBackground,
  surface = JarvisSurface,
  onSurface = JarvisOnSurface,
  surfaceVariant = JarvisSurfaceVariant,
  onSurfaceVariant = JarvisOnSurfaceVariant,
  outline = JarvisOutline,
  error = JarvisError
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to futuristic dark mode for JARVIS
  dynamicColor: Boolean = false, // Preserve JARVIS cyber aesthetic by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      else -> JarvisDarkColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

