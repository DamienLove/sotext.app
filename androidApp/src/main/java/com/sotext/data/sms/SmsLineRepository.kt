package com.sotext.data.sms

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.sotext.domain.model.SmsLine
import com.sotext.domain.model.SmsLineDevice
import com.sotext.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class SmsLineRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsRepository: SettingsRepository
) {
    private companion object {
        const val TAG = "SmsLineRepository"
    }
    fun observeLines(): Flow<List<SmsLine>> = callbackFlow {
        var listener: ListenerRegistration? = null
        val authListener = FirebaseAuth.AuthStateListener { state ->
            listener?.remove()
            listener = null
            val user = state.currentUser
            if (user == null) {
                trySend(emptyList())
                return@AuthStateListener
            }
            val ref = firestore.collection("users").document(user.uid).collection("lines")
            listener = ref.addSnapshotListener { snapshot, _ ->
                val lines = snapshot?.documents?.mapNotNull { doc ->
                    val phoneNumber = doc.getString("phoneNumber").orEmpty()
                    val createdAt = (doc.getTimestamp("createdAt") ?: doc.getTimestamp("updatedAt"))?.toDate()?.time ?: 0L
                    val disabled = doc.getBoolean("disabled") == true
                    SmsLine(
                        id = doc.id,
                        phoneNumber = phoneNumber,
                        label = doc.getString("label"),
                        primaryDeviceId = doc.getString("primaryDeviceId").orEmpty(),
                        createdAt = createdAt,
                        disabled = disabled
                    )
                } ?: emptyList()
                trySend(lines)
            }
        }
        auth.addAuthStateListener(authListener)
        authListener.onAuthStateChanged(auth)
        awaitClose {
            listener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    fun observeDevices(): Flow<List<SmsLineDevice>> = callbackFlow {
        var listener: ListenerRegistration? = null
        val authListener = FirebaseAuth.AuthStateListener { state ->
            listener?.remove()
            listener = null
            val user = state.currentUser
            if (user == null) {
                trySend(emptyList())
                return@AuthStateListener
            }
            val ref = firestore.collection("users").document(user.uid).collection("devices")
            listener = ref.addSnapshotListener { snapshot, _ ->
                val devices = snapshot?.documents?.mapNotNull { doc ->
                    SmsLineDevice(
                        id = doc.id,
                        lineId = doc.getString("lineId").orEmpty(),
                        phoneNumber = doc.getString("phoneNumber"),
                        displayName = doc.getString("deviceName"),
                        isPrimary = doc.getBoolean("isPrimary") == true,
                        lastSeen = doc.getTimestamp("lastSeen")?.toDate()?.time
                    )
                } ?: emptyList()
                trySend(devices)
            }
        }
        auth.addAuthStateListener(authListener)
        authListener.onAuthStateChanged(auth)
        awaitClose {
            listener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    suspend fun ensureDeviceLine(phoneNumber: String?): String? {
        val user = auth.currentUser ?: return null
        val deviceId = settingsRepository.ensureDeviceId()
        val normalized = phoneNumber?.let { normalizePhone(it) }.orEmpty()
        val linesRef = firestore.collection("users").document(user.uid).collection("lines")
        val existingLineId = if (normalized.isNotBlank()) {
            val normalizedSnapshot = runCatching {
                linesRef.whereEqualTo("phoneNumberNormalized", normalized).get().await()
            }.getOrNull()
            normalizedSnapshot?.documents?.firstOrNull()?.id
                ?: runCatching {
                    linesRef.whereEqualTo("phoneNumber", phoneNumber).get().await()
                }.getOrNull()?.documents?.firstOrNull()?.id
        } else {
            null
        }
        val lineId = existingLineId ?: if (normalized.isNotBlank()) linesRef.document().id else deviceId
        return try {
            val linePayload = mutableMapOf<String, Any>(
                "primaryDeviceId" to deviceId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            phoneNumber?.takeIf { it.isNotBlank() }?.let { linePayload["phoneNumber"] = it }
            if (normalized.isNotBlank()) {
                linePayload["phoneNumberNormalized"] = normalized
            }
            if (existingLineId == null) {
                linePayload["createdAt"] = FieldValue.serverTimestamp()
            }
            val lineRef = linesRef.document(lineId)
            lineRef.set(linePayload, SetOptions.merge()).await()

            val devicePayload = mutableMapOf<String, Any>(
                "lineId" to lineId,
                "isPrimary" to true,
                "lastSeen" to FieldValue.serverTimestamp(),
                "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            )
            phoneNumber?.takeIf { it.isNotBlank() }?.let { devicePayload["phoneNumber"] = it }
            if (normalized.isNotBlank()) {
                devicePayload["phoneNumberNormalized"] = normalized
            }
            val deviceRef = firestore.collection("users").document(user.uid).collection("devices").document(deviceId)
            deviceRef.set(devicePayload, SetOptions.merge()).await()
            lineId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure device line", e)
            null
        }
    }

    suspend fun updateDeviceLine(deviceId: String, lineId: String) {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users").document(user.uid)
                .collection("devices").document(deviceId)
                .set(
                    mapOf(
                        "lineId" to lineId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update device line for $deviceId", e)
        }
    }

    suspend fun updateDevicePresence(activeLineId: String?, phoneNumber: String?) {
        val user = auth.currentUser ?: return
        val deviceId = settingsRepository.ensureDeviceId()
        val payload = mutableMapOf<String, Any>(
            "lastSeen" to FieldValue.serverTimestamp()
        )
        activeLineId?.takeIf { it.isNotBlank() }?.let { payload["activeLineId"] = it }
        phoneNumber?.takeIf { it.isNotBlank() }?.let { payload["phoneNumber"] = it }
        try {
            firestore.collection("users").document(user.uid)
                .collection("devices").document(deviceId)
                .set(payload, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update device presence", e)
        }
    }

    suspend fun setLineDisabled(lineId: String, disabled: Boolean) {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users").document(user.uid)
                .collection("lines").document(lineId)
                .set(mapOf("disabled" to disabled, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set line disabled", e)
        }
    }

    private fun normalizePhone(input: String): String {
        return input.filter { it.isDigit() || it == '+' }.trim()
    }
}
