package com.pulselink.data.emergency

import java.util.Locale

data class EmergencyAlertLocation(
    val lat: Double,
    val lng: Double,
    val severity: String
)

private val MAPS_LINK_RE = Regex(
    "https?://maps\\.google\\.com/\\?q=([-\\d.]+),([-\\d.]+)",
    RegexOption.IGNORE_CASE
)

fun parseEmergencyAlertLocation(body: String): EmergencyAlertLocation? {
    if (body.isBlank()) return null
    val match = MAPS_LINK_RE.find(body) ?: return null
    val lat = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return null
    val lng = match.groupValues.getOrNull(2)?.toDoubleOrNull() ?: return null
    val severity = classifyAlertSeverity(body)
    return EmergencyAlertLocation(lat = lat, lng = lng, severity = severity)
}

private fun classifyAlertSeverity(body: String): String {
    val normalized = body.uppercase(Locale.US)
    return when {
        normalized.contains("PULSELINK EMERGENCY") -> "emergency"
        normalized.contains("I'M SAFE") || normalized.contains("CHECK-IN") || normalized.contains("CHECK IN") -> "check_in"
        else -> "non_urgent"
    }
}
