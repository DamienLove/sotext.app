package com.sotext.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.sotext.domain.model.ThemePreferences
import kotlin.math.max
import kotlin.math.min

fun parseColorOr(default: Color, hex: String?): Color {
    if (hex.isNullOrBlank()) return default
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(default)
}

/**
 * A theme's background gradient stops, in order, or null if it uses a flat
 * [ThemePreferences.backgroundColor] instead. Every gradient render site (inbox, thread, Visual
 * Settings' screen background/live preview/theme-card preview, and the "is a gradient active"
 * checks) goes through this one place, so they can't render - or disagree about - a theme's
 * gradient differently from each other.
 *
 * [ThemePreferences.appBackgroundGradientStops] (2+ entries) takes precedence for gradients with
 * more than 3 stops; otherwise falls back to the 2-stop start/end or 3-stop start/mid/end fields
 * the Customize tab's manual editor exposes.
 */
fun themeGradientColors(theme: ThemePreferences, alpha: Float = 1f): List<Color>? {
    val explicitStops = theme.appBackgroundGradientStops
    val stops = if (explicitStops != null && explicitStops.size >= 2) {
        explicitStops
    } else {
        val start = theme.appBackgroundGradientStart ?: return null
        val end = theme.appBackgroundGradientEnd ?: return null
        listOfNotNull(start, theme.appBackgroundGradientMid, end)
    }
    return stops.map { parseColorOr(Color.White, it).copy(alpha = alpha) }
}

/**
 * Returns a text color that stays legible on the given [background].
 * Falls back to black/white or [fallback] if the requested [desired] color has low contrast.
 */
fun ensureReadableOnColor(
    background: Color,
    desired: Color,
    fallback: Color = Color.Unspecified,
    minimumContrast: Float = 3.0f
): Color {
    fun contrast(a: Color, b: Color): Float {
        val l1 = a.luminance() + 0.05f
        val l2 = b.luminance() + 0.05f
        return max(l1, l2) / min(l1, l2)
    }

    val candidates = buildList {
        add(desired)
        add(Color.White)
        add(Color.Black)
        if (fallback != Color.Unspecified) add(fallback)
    }

    val best = candidates.maxBy { contrast(background, it) }
    val bestContrast = contrast(background, best)
    return if (bestContrast >= minimumContrast) best else best
}
