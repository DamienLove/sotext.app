package com.pulselink.data.sms

object OtpHelper {
    private val otpRegex = Regex("\\b\\d{4,8}\\b")
    private val otpKeywords = listOf(
        "code",
        "otp",
        "passcode",
        "verification",
        "verify",
        "login",
        "one-time",
        "one time",
        "2-step",
        "two-step"
    )
    private val urgentKeywords = listOf(
        "urgent",
        "asap",
        "immediately",
        "right away"
    )

    fun extractCode(body: String): String? = otpRegex.find(body)?.value

    fun isOtpMessage(address: String?, body: String): Boolean {
        if (body.isBlank()) return false
        val normalizedBody = body.lowercase()
        val hasKeyword = otpKeywords.any { normalizedBody.contains(it) }
        val hasCode = otpRegex.containsMatchIn(body)
        val shortCode = address?.let { addr ->
            val digits = addr.filter { it.isDigit() }
            digits.length in 4..8
        } ?: false
        return hasCode && (hasKeyword || shortCode)
    }

    fun isUrgentBody(body: String): Boolean {
        if (body.isBlank()) return false
        val normalized = body.lowercase()
        return urgentKeywords.any { normalized.contains(it) }
    }
}
