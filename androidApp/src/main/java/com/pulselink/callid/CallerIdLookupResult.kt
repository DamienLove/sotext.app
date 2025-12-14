package com.pulselink.callid

data class CallerIdLookupResult(
    val number: String,
    val carrier: String?,
    val location: String?,
    val lineType: String?,
    val valid: Boolean
) {
    val isLikelySpam: Boolean =
        !valid || lineType.equals("premium_rate", ignoreCase = true) ||
            lineType.equals("special_services", ignoreCase = true)

    val summary: String
        get() = listOfNotNull(location, carrier, lineType)
            .joinToString(separator = " • ")
}
