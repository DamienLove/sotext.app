package com.sotext.data.ai

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.sotext.auth.FirebaseAuthManager
import com.sotext.data.intelligence.Actionability
import com.sotext.data.intelligence.MessageEntities
import com.sotext.data.intelligence.MessageIntent
import com.sotext.data.intelligence.SafetyAnalysis
import com.sotext.data.intelligence.SafetyResponseCategory
import com.sotext.data.intelligence.SafetySignal
import com.sotext.domain.model.CatchUpCategory
import com.sotext.domain.model.CatchUpResult
import com.sotext.domain.model.MessageUrgency
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.tasks.await

@Singleton
class AiAssistantRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val authManager: FirebaseAuthManager
) : AiAssistantRepository {

    override suspend fun summarizeThread(messages: List<String>, contactName: String?): String {
        authManager.ensureSignedIn()
        val payload = mapOf(
            "messages" to messages,
            "contactName" to contactName
        )
        val data = callFunction("summarizeSmsThread", payload)
        return data["summary"] as? String ?: ""
    }

    override suspend fun composeSuggestion(
        action: AiComposeAction,
        draft: String?,
        lastMessage: String?
    ): String {
        authManager.ensureSignedIn()
        val payload = mapOf(
            "action" to action.apiValue,
            "draft" to draft,
            "lastMessage" to lastMessage
        )
        val data = callFunction("composeSmsAssist", payload)
        return data["text"] as? String ?: ""
    }

    override suspend fun classifyUrgency(message: String): AiUrgencyResult {
        authManager.ensureSignedIn()
        val payload = mapOf(
            "message" to message
        )
        val data = callFunction("classifySmsUrgency", payload)
        val urgencyRaw = (data["urgency"] as? String).orEmpty().lowercase()
        val confidence = (data["confidence"] as? Number)?.toFloat() ?: 0f
        val urgency = when (urgencyRaw) {
            "emergency" -> MessageUrgency.EMERGENCY
            "urgent" -> MessageUrgency.URGENT
            else -> MessageUrgency.STANDARD
        }
        return AiUrgencyResult(urgency = urgency, confidence = confidence)
    }

    override suspend fun classifySafety(message: String): SafetyAnalysis {
        authManager.ensureSignedIn()
        val payload = mapOf("message" to message)
        val data = callFunction("classifySmsUrgency", payload)
        val urgencyRaw = (data["urgency"] as? String).orEmpty().lowercase()
        val confidence = (data["confidence"] as? Number)?.toFloat() ?: 0f
        val level = when (urgencyRaw) {
            "emergency" -> MessageUrgency.EMERGENCY
            "urgent" -> MessageUrgency.URGENT
            else -> MessageUrgency.STANDARD
        }
        val signals = (data["signals"] as? List<*>)
            ?.filterIsInstance<String>()
            ?.mapNotNull { raw -> runCatching { SafetySignal.valueOf(raw.uppercase()) }.getOrNull() }
            ?: emptyList()
        val recommendedResponse = (data["recommendedResponse"] as? String)?.let { raw ->
            runCatching { SafetyResponseCategory.valueOf(raw.uppercase()) }.getOrNull()
        } ?: SafetyResponseCategory.MONITOR
        return SafetyAnalysis(
            level = level,
            confidence = confidence,
            signals = signals,
            recommendedResponse = recommendedResponse
        )
    }

    override suspend fun classifyMessageIntent(message: String): AiMessageIntentResult {
        authManager.ensureSignedIn()
        val payload = mapOf("message" to message)
        val data = callFunction("classifyMessageIntent", payload)
        return parseIntentResult(data) ?: AiMessageIntentResult(
            intent = MessageIntent.NONE,
            confidence = 0f,
            actionability = Actionability.NONE,
            entities = MessageEntities()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseIntentResult(data: Map<String, Any?>): AiMessageIntentResult? {
        val intentRaw = (data["intent"] as? String)?.uppercase() ?: return null
        val intent = runCatching { MessageIntent.valueOf(intentRaw) }.getOrDefault(MessageIntent.NONE)
        val confidence = (data["confidence"] as? Number)?.toFloat() ?: 0f
        val actionabilityRaw = (data["actionability"] as? String)?.uppercase()
        val actionability = actionabilityRaw?.let { runCatching { Actionability.valueOf(it) }.getOrNull() }
            ?: Actionability.NONE
        val entities = parseEntities(data["entities"] as? Map<String, Any?>)
        val secondaryRaw = data["secondaryIntent"] as? Map<String, Any?>
        val secondary = secondaryRaw?.let { raw ->
            val secIntentRaw = (raw["intent"] as? String)?.uppercase() ?: return@let null
            val secIntent = runCatching { MessageIntent.valueOf(secIntentRaw) }.getOrDefault(MessageIntent.NONE)
            AiSecondaryIntentResult(
                intent = secIntent,
                confidence = (raw["confidence"] as? Number)?.toFloat() ?: 0f,
                actionability = (raw["actionability"] as? String)?.uppercase()?.let {
                    runCatching { Actionability.valueOf(it) }.getOrNull()
                } ?: Actionability.NONE,
                entities = parseEntities(raw["entities"] as? Map<String, Any?>)
            )
        }
        return AiMessageIntentResult(
            intent = intent,
            confidence = confidence,
            actionability = actionability,
            entities = entities,
            secondaryIntent = secondary
        )
    }

    private fun parseEntities(raw: Map<String, Any?>?): MessageEntities {
        if (raw == null) return MessageEntities()
        return MessageEntities(
            person = raw["person"] as? String,
            rawDateTimeText = listOfNotNull(raw["date"] as? String, raw["time"] as? String)
                .joinToString(" ").ifBlank { null },
            location = raw["location"] as? String,
            amount = (raw["amount"] as? Number)?.toDouble(),
            task = raw["task"] as? String,
            event = raw["event"] as? String,
            organization = raw["organization"] as? String,
            phoneNumber = raw["phoneNumber"] as? String,
            email = raw["email"] as? String,
            url = raw["url"] as? String,
            deadline = raw["deadline"] as? String
        )
    }

    override suspend fun catchMeUp(conversations: List<CatchUpConversationInput>): List<CatchUpResult> {
        if (conversations.isEmpty()) return emptyList()
        authManager.ensureSignedIn()
        val payload = mapOf(
            "conversations" to conversations.map { conversation ->
                mapOf(
                    "threadId" to conversation.threadId.toString(),
                    "contactName" to conversation.contactName,
                    "messages" to conversation.messages
                )
            }
        )
        val data = callFunction("catchMeUp", payload)
        val results = data["results"] as? List<*> ?: return emptyList()
        return results.mapNotNull { raw -> parseCatchUpResult(raw) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseCatchUpResult(raw: Any?): CatchUpResult? {
        val map = raw as? Map<*, *> ?: return null
        val threadId = (map["threadId"] as? String)?.toLongOrNull() ?: return null
        val category = when ((map["category"] as? String).orEmpty()) {
            "needs_response" -> CatchUpCategory.NEEDS_RESPONSE
            "important_update" -> CatchUpCategory.IMPORTANT_UPDATE
            else -> CatchUpCategory.NO_ACTION
        }
        return CatchUpResult(
            threadId = threadId,
            category = category,
            topic = (map["topic"] as? String).orEmpty(),
            summary = (map["summary"] as? String).orEmpty(),
            needsResponse = (map["needsResponse"] as? Boolean) ?: (category == CatchUpCategory.NEEDS_RESPONSE),
            questions = (map["questions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            actionItems = (map["actionItems"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            mentionedWhen = map["mentionedWhen"] as? String
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun callFunction(name: String, payload: Map<String, Any?>): Map<String, Any?> {
        return try {
            val result = functions.getHttpsCallable(name).call(payload).await()
            result.data as? Map<String, Any?> ?: emptyMap()
        } catch (error: FirebaseFunctionsException) {
            Log.w(TAG, "AI function $name failed code=${error.code}", error)
            val message = when (error.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "Sign in to use AI features."
                FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                    "AI access denied for this account."
                FirebaseFunctionsException.Code.NOT_FOUND,
                FirebaseFunctionsException.Code.UNIMPLEMENTED ->
                    "AI backend not deployed yet."
                FirebaseFunctionsException.Code.UNAVAILABLE ->
                    "AI service unavailable. Try again shortly."
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    "AI timed out. Try again."
                else -> "AI request failed."
            }
            throw IllegalStateException(message, error)
        }
    }

    companion object {
        private const val TAG = "AiAssistantRepository"
    }
}
