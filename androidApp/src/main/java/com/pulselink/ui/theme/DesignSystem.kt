package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Simple shared color tokens pulled from existing theme values to avoid duplication.
// These mirror the palette used in PulseLinkTheme.
private val BackgroundDark = Color(0xFF0B0E16)
private val BrandPrimaryDark = Color(0xFF1A237E)
private val BrandPrimaryLight = Color(0xFF5E6FFF)
private val EmergencyRed = Color(0xFFD32F2F)
private val EmergencyDarkRed = Color(0xFF9A0007)

object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val section = 48.dp
}

object Layout {
    val cardCornerRadius = 16.dp
    val buttonCornerRadius = 12.dp
    val inputCornerRadius = 12.dp
    val bottomSheetCornerRadius = 24.dp
}

object Gradients {
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(BackgroundDark, Color(0xFF0B0D16))
    )
    
    val BrandGradient = Brush.horizontalGradient(
        colors = listOf(BrandPrimaryDark, BrandPrimaryLight)
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(EmergencyRed, EmergencyDarkRed)
    )
}

object Elevations {
    val card = 2.dp
    val floating = 6.dp
}
