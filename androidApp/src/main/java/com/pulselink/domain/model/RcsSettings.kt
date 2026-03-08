package com.pulselink.domain.model

import kotlinx.serialization.Serializable

/**
 * Settings for RCS (Rich Communication Services) features.
 * All sub-settings are only effective when [rcsEnabled] is true.
 */
@Serializable
data class RcsSettings(
    /** Master toggle - controls all RCS functionality */
    val rcsEnabled: Boolean = false,

    /** Send read receipts to others when you read their messages */
    val sendReadReceipts: Boolean = true,

    /** Receive read receipts from others when they read your messages */
    val receiveReadReceipts: Boolean = true,

    /** Show others when you are typing a message */
    val sendTypingIndicators: Boolean = true,

    /** See when others are typing a message to you */
    val receiveTypingIndicators: Boolean = true,

    /** Share photos and videos in full resolution */
    val highResolutionMedia: Boolean = true,

    /** Show delivery status confirmations for sent messages */
    val showDeliveryStatus: Boolean = true
) {
    /** Effective read receipts sending - only active when RCS is enabled and setting is on */
    val effectiveSendReadReceipts: Boolean
        get() = rcsEnabled && sendReadReceipts

    /** Effective typing indicators sending - only active when RCS is enabled and setting is on */
    val effectiveSendTypingIndicators: Boolean
        get() = rcsEnabled && sendTypingIndicators

    /** Effective high-res media - only active when RCS is enabled and setting is on */
    val effectiveHighResolutionMedia: Boolean
        get() = rcsEnabled && highResolutionMedia

    /** Effective delivery status - only active when RCS is enabled and setting is on */
    val effectiveShowDeliveryStatus: Boolean
        get() = rcsEnabled && showDeliveryStatus
}

/**
 * Represents device RCS capability status.
 */
data class RcsCapability(
    /** Whether RCS features are available on this device */
    val isAvailable: Boolean,

    /** Whether RCS is currently enabled/active */
    val isEnabled: Boolean,

    /** Carrier name if available */
    val carrierName: String? = null,

    /** Reason why RCS is unavailable, if applicable */
    val errorReason: String? = null
)
