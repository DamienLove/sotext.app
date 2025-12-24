package com.pulselink.util

import android.telephony.PhoneNumberUtils

private const val DISPLAY_DELIMITER = " | "
private val LEGACY_DELIMITERS = listOf(
    DISPLAY_DELIMITER,
    " \u2022 ",
    " \u00b7 ",
    " \u00fa ",
    " \u0141 ",
    " \u0007 ",
    " L "
)

fun formatSmsDisplayName(name: String, number: String): String {
    return "$name$DISPLAY_DELIMITER$number"
}

fun splitSmsDisplayAddress(raw: String): Pair<String, String?> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "" to null
    LEGACY_DELIMITERS.forEach { delimiter ->
        val index = trimmed.indexOf(delimiter)
        if (index > 0) {
            val left = trimmed.substring(0, index).trim()
            val right = trimmed.substring(index + delimiter.length).trim()
            if (right.isNotBlank() && right.count { it.isDigit() } >= 2) {
                return left to right
            }
        }
    }
    return trimmed to null
}

fun stripSmsDisplayName(raw: String): String {
    val (left, right) = splitSmsDisplayAddress(raw)
    return right ?: left
}

fun normalizeSmsAddress(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""

    val cleaned = stripSmsDisplayName(trimmed)

    if (cleaned.contains(";") || cleaned.contains(",")) {
        return cleaned.split(';', ',')
            .map { normalizeSmsAddress(it) }
            .filter { it.isNotBlank() }
            .joinToString(";")
    }

    val normalized = PhoneNumberUtils.normalizeNumber(cleaned)
    return if (normalized.isNotBlank()) normalized else cleaned.lowercase()
}
