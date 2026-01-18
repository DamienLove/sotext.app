package com.pulselink.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Future Deep v8 Design System Tokens
// The ultimate unified design language for PulseLink Suite.
// V8: "Neon Noir" - deeper blacks, brighter neons, holographic finishes.

// Colors
val DeepBackground = Color(0xFF020202) // V8 Deepest Black
val DeepSurface = Color(0xFF080B14) // V8 Surface (Darker)
val DeepSurfaceAlt = Color(0xFF0F1521) // V8 Surface Alt
val DeepAccent = Color(0xFF00FFFF) // V8 Neon Cyan
val DeepAccentStrong = Color(0xFF00B8D4) // Cyan 700
val DeepTertiary = Color(0xFFE000FF) // V8 Neon Magenta
val DeepError = Color(0xFFFF2E4D) // V8 Error (Brighter)
val DeepOnSurface = Color(0xFFE0F7FA) // V8 Ink
val DeepMuted = Color(0xFF64748B) // Slate 500

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
    // V8 "Alive" Background
    val PrimaryBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF050810), DeepBackground)
    )
    
    // Vibrant accent gradient for buttons/FABs
    val BrandGradient = Brush.linearGradient(
        colors = listOf(
            DeepAccent,
            Color(0xFF00E5FF),
            DeepAccentStrong
        )
    )

    // Futuristic Holographic Gradient
    val Holographic = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.9f),
            Color(0xFFD8B4FE).copy(alpha = 0.8f),
            DeepAccentStrong.copy(alpha = 0.9f)
        )
    )

    // "Neon Glow" for active states or borders
    val NeonGlow = Brush.horizontalGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.0f),
            DeepAccent.copy(alpha = 0.8f),
            DeepAccent.copy(alpha = 0.0f)
        )
    )

    // V8 Neon Border - sharper
    val NeonBorder = Brush.linearGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.6f),
            DeepTertiary.copy(alpha = 0.4f),
            DeepAccent.copy(alpha = 0.6f)
        )
    )

    // V8 Holographic Border (New)
    val HolographicBorder = Brush.sweepGradient(
        colors = listOf(
            DeepAccent.copy(alpha = 0.3f),
            DeepTertiary.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.5f), // Shine
            DeepAccent.copy(alpha = 0.3f)
        )
    )

    val EmergencyGradient = Brush.linearGradient(
        colors = listOf(DeepError, Color(0xFF991B1B))
    )

    // Refined V8 Glass Border
    val GlassBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.02f)
        )
    )

    val SurfaceShine = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.0f)
        )
    )
}

object Elevations {
    val flat = 0.dp
    val card = 0.dp
    val floating = 12.dp
    val sticky = 4.dp
}
