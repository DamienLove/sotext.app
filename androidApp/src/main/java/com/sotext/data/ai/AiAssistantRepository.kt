package com.sotext.data.ai

import com.sotext.data.intelligence.Actionability
import com.sotext.data.intelligence.MessageEntities
import com.sotext.data.intelligence.MessageIntent
import com.sotext.data.intelligence.SafetyAnalysis
import com.sotext.domain.model.CatchUpResult
import com.sotext.domain.model.MessageUrgency

data class AiUrgencyResult(
    val urgency: MessageUrgency,
    val confidence: Float
)

/** Cloud deep-pass intent classification for one message, before a messageId is attached. */
data class AiMessageIntentResult(
    val intent: MessageIntent,
    val confidence: Float,
    val actionability: Actionability,
    val entities: MessageEntities,
    val secondaryIntent: AiSecondaryIntentResult? = null
)

data class AiSecondaryIntentResult(
    val intent: MessageIntent,
    val confidence: Float,
    val actionability: Actionability,
    val entities: MessageEntities
)

enum class AiComposeAction(val apiValue: String, val label: String) {
    REWRITE("rewrite", "Rewrite"),
    SHORTEN("shorten", "Shorten"),
    EXPAND("expand", "Expand"),
    POLISH("polish", "Polish"),
    URGENT("urgent", "Make urgent"),
    REPLY("reply", "Reply")
}

/** One conversation's worth of context to feed the `catchMeUp` briefing flow. */
data class CatchUpConversationInput(
    val threadId: Long,
    val contactName: String?,
    val messages: List<String>
)

interface AiAssistantRepository {
    suspend fun summarizeThread(messages: List<String>, contactName: String?): String
    suspend fun composeSuggestion(
        action: AiComposeAction,
        draft: String?,
        lastMessage: String?
    ): String
    suspend fun classifyUrgency(message: String): AiUrgencyResult
    /** Same cloud classifier as [classifyUrgency], but reads the additive safety fields
     *  (signals/recommendedResponse) it also returns - see `classifySmsUrgencyFlow`. */
    suspend fun classifySafety(message: String): SafetyAnalysis
    suspend fun classifyMessageIntent(message: String): AiMessageIntentResult
    suspend fun catchMeUp(conversations: List<CatchUpConversationInput>): List<CatchUpResult>
}
