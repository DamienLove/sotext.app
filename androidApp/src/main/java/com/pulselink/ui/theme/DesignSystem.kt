package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// SoText Horizon design tokens

val DeepBackground = Color(0xFF070A12)
val DeepSurface = Color(0xFF101726)
val DeepSurfaceAlt = Color(0xFF172136)
val DeepAccent = Color(0xFF4EE3A1)
val DeepAccentStrong = Color(0xFF11B87C)
val DeepTertiary = Color(0xFFFFB75E)
val DeepError = Color(0xFFFF5E6A)
val DeepOnSurface = Color(0xFFF4F7FF)
val DeepMuted = Color(0xFF9AA7C2)

object Spacing {
    val extraSmall = 6.dp
    val small = 10.dp
    val medium = 18.dp
    val large = 26.dp
    val extraLarge = 40.dp
    val section = 64.dp
}

object Layout {
    val cardCornerRadius = 26.dp
    val buttonCornerRadius = 18.dp
    val inputCornerRadius = 18.dp
    val bottomSheetCornerRadius = 36.dp
    val dialogCornerRadius = 28.dp
}

object Gradients {
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF101728), DeepBackground)
    )

    val BrandGradient = Brush.linearGradient(
        colors = listOf(
            DeepAccent,
            DeepAccentStrong,
            DeepTertiary
        )
    )

    val Holographic = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.85f),
            Color(0xFF5AC8FA).copy(alpha = 0.8f),
            DeepTertiary.copy(alpha = 0.9f)
        )
    )

    val NeonGlow = Brush.horizontalGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.0f),
            DeepAccent.copy(alpha = 0.75f),
            DeepAccent.copy(alpha = 0.0f)
        )
    )

    val NeonBorder = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.65f),
            DeepTertiary.copy(alpha = 0.5f),
            DeepAccent.copy(alpha = 0.65f)
        )
    )

    val HolographicBorder = Brush.sweepGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.45f),
            DeepTertiary.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.65f),
            DeepAccent.copy(alpha = 0.45f)
        )
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(DeepError, Color(0xFF7A1824))
    )

    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f)
        )
    )

    val SurfaceShine = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.0f)
        )
    )

    val HolographicSurface = Brush.linearGradient(
        colors = listOf(
            DeepSurfaceAlt.copy(alpha = 0.9f),
            DeepSurface.copy(alpha = 0.96f)
        )
    )
}

object Elevations {
    val flat = 0.dp
    val card = 0.dp
    val floating = 12.dp
    val sticky = 4.dp
}
