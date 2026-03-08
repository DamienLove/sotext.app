package com.pulselink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = DeepAccent,
    onPrimary = Color(0xFF032016),
    primaryContainer = Color(0xFF154835),
    onPrimaryContainer = Color(0xFFD8FBEA),
    secondary = DeepTertiary,
    onSecondary = Color(0xFF2B1500),
    secondaryContainer = Color(0xFF5A340A),
    onSecondaryContainer = Color(0xFFFFE4BE),
    tertiary = Color(0xFF74B9FF),
    onTertiary = Color(0xFF041A30),
    background = DeepBackground,
    onBackground = DeepOnSurface,
    surface = DeepSurface,
    onSurface = DeepOnSurface,
    surfaceVariant = DeepSurfaceAlt,
    onSurfaceVariant = DeepMuted,
    error = DeepError,
    onError = Color(0xFF2D0B10)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F8D60),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFB56A16),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F8FF),
    onSurface = Color(0xFF131A2B),
    background = Color(0xFFF4F7FE),
    onBackground = Color(0xFF121A2A),
    surfaceVariant = Color(0xFFE4EAF8),
    onSurfaceVariant = Color(0xFF4C5B76),
    error = Color(0xFFD63F4F),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun PulseLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
