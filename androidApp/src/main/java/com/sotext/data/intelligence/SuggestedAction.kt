package com.sotext.data.intelligence

/**
 * A concrete, on-device action a Smart Message Card can offer. No type here requires a
 * third-party integration (spec section 5) - [OPEN_PAYMENT_APP] is a stub the UI renders as
 * disabled/informational until a real payment integration exists, so the architecture doesn't
 * need to change when one is added.
 */
enum class SuggestedActionType {
    CREATE_REMINDER,
    ADD_TO_CALENDAR,
    SHARE_LOCATION,
    CHECK_AVAILABILITY,
    SUGGEST_REPLY,
    FIND_CONTACT,
    REPLY,
    OPEN_PAYMENT_APP,
    GET_HELP,
    DISMISS
}

data class SuggestedAction(
    val type: SuggestedActionType,
    val label: String,
    val isPrimary: Boolean = false
)
