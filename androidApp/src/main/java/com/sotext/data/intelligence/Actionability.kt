package com.sotext.data.intelligence

/**
 * How directly a message asks for something, distinct from *what* it's asking for
 * ([MessageIntent]). This is what keeps "I need to remember to buy milk." (a statement) from
 * auto-creating a reminder the way "Remind me to buy milk tonight." (an explicit request) does -
 * see [MessageIntentDetector] and [MessageIntelligenceDecisionEngine].
 */
enum class Actionability {
    /** A clear, direct request/command - e.g. "Remind me to...", "Can you send me...". */
    EXPLICIT,

    /** Task-like language without a direct request - e.g. "I need to remember to...". */
    IMPLIED,

    /** States a fact or shares information; nothing to act on. */
    INFORMATIONAL,

    /** No actionable or task-like language detected at all. */
    NONE,

    /** Detected something, but not confidently enough to call it explicit or implied. */
    AMBIGUOUS
}
