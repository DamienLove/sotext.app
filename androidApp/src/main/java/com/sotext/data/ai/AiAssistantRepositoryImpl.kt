package com.sotext.data.ai

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.sotext.auth.FirebaseAuthManager
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
