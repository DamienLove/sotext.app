package com.pulselink.util

import android.telephony.PhoneNumberUtils

fun normalizeSmsAddress(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""

    val delimiter = when {
        trimmed.contains(" • ") -> " • "
        trimmed.contains(" Ł ") -> " Ł "
        trimmed.contains(" L ") -> " L "
        else -> null
    }
    val cleaned = if (delimiter != null) {
        trimmed.split(delimiter, limit = 2).getOrNull(1)?.trim().orEmpty()
    } else {
        trimmed
    }

    if (cleaned.contains(";") || cleaned.contains(",")) {
        return cleaned.split(';', ',')
            .map { normalizeSmsAddress(it) }
            .filter { it.isNotBlank() }
            .joinToString(";")
    }

    val normalized = PhoneNumberUtils.normalizeNumber(cleaned)
    return if (normalized.isNotBlank()) normalized else cleaned.lowercase()
}
