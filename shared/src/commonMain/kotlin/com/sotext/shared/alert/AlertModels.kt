package com.sotext.shared.alert

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AlertSeverity {
    @SerialName("emergency")
    EMERGENCY,

    @SerialName("non_urgent")
    NON_URGENT,

    @SerialName("check_in")
    CHECK_IN
}

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null
)

@Serializable
data class AlertRecipient(
    val phoneNumber: String? = null,
    val pushToken: String? = null,
    val email: String? = null
)

@Serializable
data class AlertRequest(
    val message: String,
    val severity: AlertSeverity,
    val recipients: List<AlertRecipient>,
    val senderId: String? = null,
    val location: GeoPoint? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class AlertResponse(
    val status: String,
    val relayId: String? = null,
    val estimatedFanOut: Int? = null
)
