package com.sotext.data.ai

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.sotext.auth.FirebaseAuthManager
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
