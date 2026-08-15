package com.sotext.data.ai

import com.sotext.domain.model.MessageUrgency

data class AiUrgencyResult(
    val urgency: MessageUrgency,
    val confidence: Float
)

enum class AiComposeAction(val apiValue: String, val label: String) {
    REWRITE("rewrite", "Rewrite"),
    SHORTEN("shorten", "Shorten"),
    EXPAND("expand", "Expand"),
    POLISH("polish", "Polish"),
    URGENT("urgent", "Make urgent"),
    REPLY("reply", "Reply")
}

interface AiAssistantRepository {
    suspend fun summarizeThread(messages: List<String>, contactName: String?): String
    suspend fun composeSuggestion(
        action: AiComposeAction,
        draft: String?,
        lastMessage: String?
    ): String
    suspend fun classifyUrgency(message: String): AiUrgencyResult
}
