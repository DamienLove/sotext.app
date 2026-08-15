package com.sotext.data.sms

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sotext.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class SmsOutboxService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsRepository: SettingsRepository
) {
    private companion object {
        const val TAG = "SmsOutboxService"
    }

    suspend fun queueMessage(address: String, body: String, lineId: String, threadId: Long? = null) {
        val user = auth.currentUser ?: return
        val deviceId = settingsRepository.ensureDeviceId()
        val payload = mutableMapOf<String, Any>(
            "address" to address,
            "body" to body,
            "lineId" to lineId,
            "fromDeviceId" to deviceId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        threadId?.let { payload["threadId"] = it }
        try {
            firestore.collection("users").document(user.uid)
                .collection("outbox")
                .add(payload)
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to queue outbox message", e)
        }
    }
}
