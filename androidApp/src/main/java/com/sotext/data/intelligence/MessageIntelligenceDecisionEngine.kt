package com.sotext.data.intelligence

import com.sotext.domain.model.MessageUrgency

/**
 * What, if anything, should render for a message - at most one primary card, per spec section 10.
 */
sealed class CardDecision {
    /** Safety-critical - always wins over every other outcome (spec section 9). */
    data class ShowSafetyCard(val safety: SafetyAnalysis) : CardDecision()

    /** High-confidence, explicit/actionable - shown automatically. */
    data class ShowActionCard(
        val intent: MessageIntent,
        val entities: MessageEntities,
        val actions: List<SuggestedAction>,
        val confidence: Float,
        /** A second, lower-priority intent from the same message, if any (spec section 6) -
         *  the UI renders this as an extra affordance on the same card, never a second card. */
        val secondary: SecondaryIntent? = null
    ) : CardDecision()

    /** Medium confidence - a subtle, low-emphasis suggestion rather than a full card. */
    data class ShowSuggestion(
        val intent: MessageIntent,
        val entities: MessageEntities,
        val confidence: Float
    ) : CardDecision()

    /** Low confidence, no intent, suppressed by user preference, or already dismissed. */
    object NoCard : CardDecision()
}

/**
 * The single place that turns a [MessageIntelligenceResult] into a UI decision, implementing the
 * priority order from spec section 10:
 *
 * safety-critical > explicit actionable > high-confidence task > high-confidence informational >
 * low-confidence suggestion > no card
 *
 * Pure function, no Android dependencies - fully unit-testable (see
 * `MessageIntelligenceDecisionEngineTest`). [MessageIntelligenceRepository] is the only caller.
 */
object MessageIntelligenceDecisionEngine {

    fun decide(
        result: MessageIntelligenceResult?,
        thresholds: MessageIntelligenceThresholds = MessageIntelligenceThresholds.DEFAULT,
        suppressedTypes: Set<MessageIntent> = emptySet(),
        dismissed: Boolean = false
    ): CardDecision {
        if (dismissed) return CardDecision.NoCard

        // Safety overrides everything else, including an otherwise-actionable intent on the same
        // message (spec section 9) - checked before intent/suppression/dismissal even apply.
        val safety = result?.safety
        if (safety != null && safety.level != MessageUrgency.STANDARD && safety.confidence >= thresholds.highConfidence) {
            return CardDecision.ShowSafetyCard(safety)
        }

        if (result == null || result.intent == MessageIntent.NONE) return CardDecision.NoCard
        if (result.intent in suppressedTypes) return CardDecision.NoCard

        val usableSecondary = result.secondaryIntents
            .filter { it.intent !in suppressedTypes && it.confidence >= thresholds.mediumConfidence }
            .maxByOrNull { it.confidence }

        return when {
            result.confidence >= thresholds.highConfidence && result.actionability == Actionability.EXPLICIT ->
                CardDecision.ShowActionCard(
                    intent = result.intent,
                    entities = result.entities,
                    actions = result.suggestedActions,
                    confidence = result.confidence,
                    secondary = usableSecondary
                )
            result.confidence >= thresholds.mediumConfidence ->
                CardDecision.ShowSuggestion(
                    intent = result.intent,
                    entities = result.entities,
                    confidence = result.confidence
                )
            else -> CardDecision.NoCard
        }
    }
}
