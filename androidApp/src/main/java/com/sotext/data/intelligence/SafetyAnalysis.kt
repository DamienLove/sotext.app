package com.sotext.data.intelligence

import com.sotext.domain.model.MessageUrgency

/**
 * A specific category of safety-relevant language a message may contain. Identifies signals,
 * not diagnoses - see [SafetyAnalysis] doc.
 */
enum class SafetySignal {
    THREAT,
    HARASSMENT,
    VIOLENCE_CONCERN,
    SELF_HARM_CONCERN,
    SCAM_FRAUD,
    EMERGENCY_LANGUAGE,
    OTHER
}

/** What kind of response the safety analysis suggests, in increasing order of urgency. */
enum class SafetyResponseCategory {
    MONITOR,
    OFFER_HELP,
    URGENT_ESCALATE,
    EMERGENCY_ESCALATE
}

/**
 * Safety output of the Message Intelligence pipeline - an extension of the existing urgency
 * classifier ([com.sotext.data.ai.AiAssistantRepository.classifyUrgency] /
 * `classifySmsUrgencyFlow` in `functions/src/ai.ts`), not a replacement for it. [level] reuses
 * the existing [MessageUrgency] enum that already drives DND-override behavior in
 * `PulseLinkSmsReceiver`, so both call sites agree on what "emergency" means.
 *
 * This identifies signals and recommends a proportionate response; it never claims a diagnosis
 * or a definitive judgment about the sender (spec section 8).
 */
data class SafetyAnalysis(
    val level: MessageUrgency,
    val confidence: Float,
    val signals: List<SafetySignal> = emptyList(),
    val recommendedResponse: SafetyResponseCategory = SafetyResponseCategory.MONITOR
) {
    companion object {
        val NONE = SafetyAnalysis(
            level = MessageUrgency.STANDARD,
            confidence = 1f,
            signals = emptyList(),
            recommendedResponse = SafetyResponseCategory.MONITOR
        )
    }
}
