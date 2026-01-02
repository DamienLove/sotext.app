package com.pulselink.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

fun parseColorOr(default: Color, hex: String?): Color {
    if (hex.isNullOrBlank()) return default
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(default)
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
