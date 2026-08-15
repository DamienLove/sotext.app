package com.sotext.callid

/**
 * Common caller ID contract implemented by individual providers.
 */
interface CallerIdProvider {
    val providerName: String
    val priority: Int
    suspend fun lookup(phoneNumber: String): CallerIdLookupResult?
}

data class CallerIdLookupResult(
    val number: String,
    val carrier: String?,
    val location: String?,
    val lineType: String?,
    val valid: Boolean,
    val spamScore: Int?,
    val isLikelySpam: Boolean,
    val source: String,
    val summary: String
)
