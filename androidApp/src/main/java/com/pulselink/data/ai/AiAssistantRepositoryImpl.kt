package com.pulselink.data.ai

import com.google.firebase.functions.FirebaseFunctions
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.domain.model.MessageUrgency
import javax.inject.Inject
import javax.inject.Singleton
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

    @Suppress("UNCHECKED_CAST")
    private suspend fun callFunction(name: String, payload: Map<String, Any?>): Map<String, Any?> {
        val result = functions.getHttpsCallable(name).call(payload).await()
        return result.data as? Map<String, Any?> ?: emptyMap()
    }
}
