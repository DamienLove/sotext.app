package com.sotext.data.intelligence

/**
 * The Kotlin shape of the pipeline's unified per-message analysis - the on-device analog of the
 * JSON schema in the product spec (intent / confidence / actionability / entities / safety /
 * suggested actions), plus [secondaryIntents] for messages that carry more than one (spec
 * section 6). Never exposed to the user directly - [MessageIntelligenceDecisionEngine] decides
 * what UI, if any, this turns into.
 */
data class MessageIntelligenceResult(
    val messageId: Long,
    val intent: MessageIntent,
    val confidence: Float,
    val actionability: Actionability,
    val entities: MessageEntities = MessageEntities(),
    val safety: SafetyAnalysis = SafetyAnalysis.NONE,
    val suggestedActions: List<SuggestedAction> = emptyList(),
    val secondaryIntents: List<SecondaryIntent> = emptyList(),
    val source: ResultSource = ResultSource.ON_DEVICE
)

data class SecondaryIntent(
    val intent: MessageIntent,
    val confidence: Float,
    val actionability: Actionability,
    val entities: MessageEntities = MessageEntities()
)

/** Where a result came from - used by the decision engine's "AI can upgrade, never downgrade" merge rule. */
enum class ResultSource {
    ON_DEVICE,
    CLOUD
}
