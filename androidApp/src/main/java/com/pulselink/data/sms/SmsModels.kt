package com.pulselink.data.sms

data class SmsThreadItem(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val timestamp: Long,
    val unread: Boolean
)

data class SmsMessageItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val outgoing: Boolean
)
