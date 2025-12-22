package com.pulselink.ui.model

data class MessageRecipient(
    val id: Long,
    val displayName: String,
    val phoneNumber: String,
    val isTrusted: Boolean
)
