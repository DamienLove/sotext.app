package com.sotext.data.intelligence

/**
 * Confidence cutoffs the whole pipeline shares - the spec's "configurable rather than
 * hard-coded throughout the application" requirement, satisfied as one canonical source instead
 * of a constant repeated at every call site. [MessageIntelligenceDecisionEngine] is the only
 * consumer; nothing else should compare a confidence value to a raw number.
 *
 * A user-facing settings UI to tune these is a natural follow-up, not attempted in this phase -
 * for now this is a fixed default, not yet wired to [com.sotext.domain.model.PulseLinkSettings].
 */
data class MessageIntelligenceThresholds(
    /** At or above this: show the card automatically. */
    val highConfidence: Float = 0.75f,
    /** At or above this (but below [highConfidence]): show a subtle suggestion, not a full card. */
    val mediumConfidence: Float = 0.5f
) {
    companion object {
        val DEFAULT = MessageIntelligenceThresholds()
    }
}
