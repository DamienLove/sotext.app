package com.sotext.data.intelligence

import com.sotext.domain.model.MessageUrgency
import org.junit.Assert.*
import org.junit.Test

class MessageIntelligenceDecisionEngineTest {

    private fun result(
        intent: MessageIntent = MessageIntent.REMINDER,
        confidence: Float = 0.9f,
        actionability: Actionability = Actionability.EXPLICIT,
        safety: SafetyAnalysis = SafetyAnalysis.NONE,
        secondaryIntents: List<SecondaryIntent> = emptyList()
    ) = MessageIntelligenceResult(
        messageId = 1,
        intent = intent,
        confidence = confidence,
        actionability = actionability,
        entities = MessageEntities(),
        safety = safety,
        suggestedActions = listOf(SuggestedAction(SuggestedActionType.CREATE_REMINDER, "Add Reminder", isPrimary = true)),
        secondaryIntents = secondaryIntents
    )

    @Test
    fun `high-confidence explicit intent shows an action card`() {
        val decision = MessageIntelligenceDecisionEngine.decide(result())
        assertTrue(decision is CardDecision.ShowActionCard)
        assertEquals(MessageIntent.REMINDER, (decision as CardDecision.ShowActionCard).intent)
    }

    @Test
    fun `medium confidence shows a subtle suggestion, not a full card`() {
        val decision = MessageIntelligenceDecisionEngine.decide(result(confidence = 0.6f))
        assertTrue(decision is CardDecision.ShowSuggestion)
    }

    @Test
    fun `low confidence shows nothing`() {
        val decision = MessageIntelligenceDecisionEngine.decide(result(confidence = 0.3f))
        assertEquals(CardDecision.NoCard, decision)
    }

    @Test
    fun `no intent detected shows nothing`() {
        val decision = MessageIntelligenceDecisionEngine.decide(
            result(intent = MessageIntent.NONE, confidence = 0f, actionability = Actionability.NONE)
        )
        assertEquals(CardDecision.NoCard, decision)
    }

    @Test
    fun `a null result shows nothing`() {
        assertEquals(CardDecision.NoCard, MessageIntelligenceDecisionEngine.decide(null))
    }

    @Test
    fun `a high-confidence safety signal shows the safety card instead of an ordinary action card`() {
        // Spec section 9's exact scenario: an otherwise-actionable message (here, framed as an
        // explicit help request) that also carries a safety signal must show the safety card,
        // never the generic card.
        val safetyResult = result(
            intent = MessageIntent.HELP_REQUEST,
            confidence = 0.9f,
            safety = SafetyAnalysis(
                level = MessageUrgency.EMERGENCY,
                confidence = 0.92f,
                signals = listOf(SafetySignal.THREAT, SafetySignal.EMERGENCY_LANGUAGE),
                recommendedResponse = SafetyResponseCategory.EMERGENCY_ESCALATE
            )
        )
        val decision = MessageIntelligenceDecisionEngine.decide(safetyResult)
        assertTrue(decision is CardDecision.ShowSafetyCard)
        assertEquals(MessageUrgency.EMERGENCY, (decision as CardDecision.ShowSafetyCard).safety.level)
    }

    @Test
    fun `an ordinary message with no safety signal never shows the safety card`() {
        val decision = MessageIntelligenceDecisionEngine.decide(result(safety = SafetyAnalysis.NONE))
        assertFalse(decision is CardDecision.ShowSafetyCard)
    }

    @Test
    fun `a low-confidence safety signal does not override an ordinary card (avoid alarmist UI)`() {
        val lowConfidenceSafety = result(
            safety = SafetyAnalysis(level = MessageUrgency.URGENT, confidence = 0.4f)
        )
        val decision = MessageIntelligenceDecisionEngine.decide(lowConfidenceSafety)
        assertFalse(decision is CardDecision.ShowSafetyCard)
    }

    @Test
    fun `a suppressed intent type shows nothing even at high confidence`() {
        val decision = MessageIntelligenceDecisionEngine.decide(
            result(),
            suppressedTypes = setOf(MessageIntent.REMINDER)
        )
        assertEquals(CardDecision.NoCard, decision)
    }

    @Test
    fun `an already-dismissed message shows nothing regardless of confidence`() {
        val decision = MessageIntelligenceDecisionEngine.decide(result(), dismissed = true)
        assertEquals(CardDecision.NoCard, decision)
    }

    @Test
    fun `multiple competing intents still produce exactly one card, with the second surfaced as a secondary`() {
        val decision = MessageIntelligenceDecisionEngine.decide(
            result(
                secondaryIntents = listOf(
                    SecondaryIntent(MessageIntent.INFORMATION_REQUEST, 0.55f, Actionability.EXPLICIT, MessageEntities())
                )
            )
        )
        assertTrue(decision is CardDecision.ShowActionCard)
        val card = decision as CardDecision.ShowActionCard
        assertEquals(MessageIntent.REMINDER, card.intent)
        assertEquals(MessageIntent.INFORMATION_REQUEST, card.secondary?.intent)
    }

    @Test
    fun `a low-confidence secondary intent is dropped rather than surfaced`() {
        val decision = MessageIntelligenceDecisionEngine.decide(
            result(
                secondaryIntents = listOf(
                    SecondaryIntent(MessageIntent.QUESTION, 0.2f, Actionability.AMBIGUOUS, MessageEntities())
                )
            )
        )
        val card = decision as CardDecision.ShowActionCard
        assertNull(card.secondary)
    }
}
