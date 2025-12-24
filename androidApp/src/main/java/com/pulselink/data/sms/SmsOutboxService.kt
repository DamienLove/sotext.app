package com.pulselink.data.sms

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.pulselink.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class SmsOutboxService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsRepository: SettingsRepository
) {
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
        firestore.collection("users").document(user.uid)
            .collection("outbox")
            .add(payload)
            .await()
    }
}
