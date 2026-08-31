package com.sotext.data.intelligence

import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for the Message Intelligence pipeline. Caches one [MessageIntelligenceResult]
 * per Telephony message id - the same `LruCache`-per-message-id pattern [com.sotext.data.sms.SmsRepository]
 * already uses, so no new Room table is needed for Phase 1.
 *
 * On-device analysis ([analyzeOnDevice]) is synchronous and free - safe to call at render time
 * for every message, same as [MessageContextParser] today. A cloud deep-pass or safety result
 * (Premium-gated, computed elsewhere via `AiAssistantRepository`) is merged in later via
 * [mergeCloudIntent]/[mergeCloudSafety], which can only *upgrade* what's cached - raise
 * confidence, add safety data, or replace a lower-confidence on-device guess - never replace an
 * already-shown result with a weaker one.
 */
@Singleton
class MessageIntelligenceRepository @Inject constructor() {

    private val cache = LruCache<Long, MessageIntelligenceResult>(200)

    fun cached(messageId: Long): MessageIntelligenceResult? = cache.get(messageId)

    fun analyzeOnDevice(
        messageId: Long,
        body: String,
        timestampMillis: Long,
        senderAddress: String? = null
    ): MessageIntelligenceResult {
        cache.get(messageId)?.let { return it }

        val matches = MessageIntentDetector.analyze(messageId, body, timestampMillis, senderAddress)
        val primary = matches.firstOrNull()
        val result = if (primary == null) {
            MessageIntelligenceResult(
                messageId = messageId,
                intent = MessageIntent.NONE,
                confidence = 0f,
                actionability = Actionability.NONE
            )
        } else {
            val secondary = matches.drop(1).firstOrNull { it.intent != primary.intent }
            MessageIntelligenceResult(
                messageId = messageId,
                intent = primary.intent,
                confidence = primary.confidence,
                actionability = primary.actionability,
                entities = primary.entities,
                suggestedActions = actionsFor(primary.intent),
                secondaryIntents = listOfNotNull(
                    secondary?.let {
                        SecondaryIntent(it.intent, it.confidence, it.actionability, it.entities)
                    }
                ),
                source = ResultSource.ON_DEVICE
            )
        }
        cache.put(messageId, result)
        return result
    }

    /** Merges a cloud safety classification into the cached result, upgrading it in place. */
    fun mergeCloudSafety(messageId: Long, safety: SafetyAnalysis): MessageIntelligenceResult {
        val existing = cache.get(messageId) ?: MessageIntelligenceResult(
            messageId = messageId,
            intent = MessageIntent.NONE,
            confidence = 0f,
            actionability = Actionability.NONE
        )
        // Never let a lower-confidence cloud read replace a safety signal already flagged.
        if (safety.confidence < existing.safety.confidence && existing.safety != SafetyAnalysis.NONE) {
            return existing
        }
        val merged = existing.copy(safety = safety, source = ResultSource.CLOUD)
        cache.put(messageId, merged)
        return merged
    }

    /** Merges a cloud intent/entity deep-pass result, only if it's a genuine upgrade over what's cached. */
    fun mergeCloudIntent(messageId: Long, cloudResult: MessageIntelligenceResult): MessageIntelligenceResult {
        val existing = cache.get(messageId)
        val merged = if (existing == null || cloudResult.confidence >= existing.confidence) {
            cloudResult.copy(safety = existing?.safety ?: cloudResult.safety, source = ResultSource.CLOUD)
        } else {
            existing
        }
        cache.put(messageId, merged)
        return merged
    }

    /** The default suggested-action set for an intent - shared by the on-device path and by
     *  callers assembling a cloud [MessageIntelligenceResult] before [mergeCloudIntent]. */
    fun actionsFor(intent: MessageIntent): List<SuggestedAction> = when (intent) {
        MessageIntent.REMINDER -> listOf(
            SuggestedAction(SuggestedActionType.CREATE_REMINDER, "Add Reminder", isPrimary = true)
        )
        MessageIntent.SCHEDULING, MessageIntent.AVAILABILITY_REQUEST -> listOf(
            SuggestedAction(SuggestedActionType.CHECK_AVAILABILITY, "Check Availability", isPrimary = true),
            SuggestedAction(SuggestedActionType.SUGGEST_REPLY, "Suggest Reply")
        )
        MessageIntent.LOCATION_REQUEST -> listOf(
            SuggestedAction(SuggestedActionType.SHARE_LOCATION, "Share Location", isPrimary = true),
            SuggestedAction(SuggestedActionType.REPLY, "Reply")
        )
        MessageIntent.CONTACT_REQUEST -> listOf(
            SuggestedAction(SuggestedActionType.FIND_CONTACT, "Find Contact", isPrimary = true),
            SuggestedAction(SuggestedActionType.REPLY, "Reply")
        )
        MessageIntent.PAYMENT_REQUEST, MessageIntent.MONEY_OWED, MessageIntent.REIMBURSEMENT_REQUEST -> listOf(
            SuggestedAction(SuggestedActionType.REPLY, "Reply", isPrimary = true),
            SuggestedAction(SuggestedActionType.OPEN_PAYMENT_APP, "Open Payment App")
        )
        MessageIntent.INFORMATION_REQUEST, MessageIntent.QUESTION -> listOf(
            SuggestedAction(SuggestedActionType.REPLY, "Reply", isPrimary = true)
        )
        else -> emptyList()
    }
}
