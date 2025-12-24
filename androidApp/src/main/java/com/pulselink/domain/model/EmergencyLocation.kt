package com.pulselink.domain.model

data class EmergencyLocation(
    val id: String,
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Double? = null,
    val severity: String = "emergency",
    val createdAt: Long = 0L,
    val sourceName: String? = null,
    val sourcePhone: String? = null,
    val incoming: Boolean = true,
    val message: String? = null,
    val clearedAt: Long? = null
)
