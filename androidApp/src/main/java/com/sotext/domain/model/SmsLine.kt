package com.sotext.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SmsLine(
    val id: String = "",
    val phoneNumber: String = "",
    val label: String? = null,
    val primaryDeviceId: String = "",
    val createdAt: Long = 0L,
    val disabled: Boolean = false
)

@Serializable
data class SmsLineDevice(
    val id: String = "",
    val lineId: String = "",
    val phoneNumber: String? = null,
    val displayName: String? = null,
    val isPrimary: Boolean = false,
    val lastSeen: Long? = null
)

@Serializable
enum class LineInboxMode {
    COMBINED,
    PER_LINE
}

@Serializable
enum class LineSendPreference {
    LAST_USED,
    DEVICE_DEFAULT,
    LINE_DEFAULT
}
