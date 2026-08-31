package com.sotext.data.intelligence

/**
 * Structured details pulled out of a message body, per spec section 4. Every field is nullable
 * and stays null unless a detector actually matched something in the text - values are never
 * invented. [dateEpochDay]/[timeMinuteOfDay] are normalized to the device's local date/time
 * (matching [com.sotext.data.context.MessageContextParser]'s own date/time handling) while
 * [rawDateTimeText] preserves the original wording for display.
 */
data class MessageEntities(
    val person: String? = null,
    val dateEpochDay: Long? = null,
    val timeMinuteOfDay: Int? = null,
    val rawDateTimeText: String? = null,
    val location: String? = null,
    val amount: Double? = null,
    val task: String? = null,
    val event: String? = null,
    val organization: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val url: String? = null,
    val deadline: String? = null
) {
    /** True once every entity a card would need to render without an unresolved prompt is present. */
    fun hasDateTime(): Boolean = dateEpochDay != null || timeMinuteOfDay != null
}
