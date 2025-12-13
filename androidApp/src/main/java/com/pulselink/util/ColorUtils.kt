package com.pulselink.util

import androidx.compose.ui.graphics.Color

fun parseColorOr(default: Color, hex: String?): Color {
    if (hex.isNullOrBlank()) return default
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(default)
}
