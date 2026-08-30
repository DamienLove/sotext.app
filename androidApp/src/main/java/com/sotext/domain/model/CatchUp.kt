package com.sotext.domain.model

/** How urgently a conversation in a Catch Me Up briefing needs the user's attention. */
enum class CatchUpCategory {
    NEEDS_RESPONSE,
    IMPORTANT_UPDATE,
    NO_ACTION
}

/**
 * One conversation's entry in a Catch Me Up briefing - grounded only in that conversation's
 * own messages, produced by the `catchMeUp` AI flow.
 */
data class CatchUpResult(
    val threadId: Long,
    val category: CatchUpCategory,
    val topic: String,
    val summary: String,
    val needsResponse: Boolean,
    val questions: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val mentionedWhen: String? = null
)
