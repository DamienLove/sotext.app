package com.pulselink.data.emergency

import android.location.Location
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EmergencyLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class EmergencyLocationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun recordOutgoing(
        location: Location,
        message: String?,
        sourceName: String?
    ) {
        val ref = collectionRef() ?: return
        val payload = hashMapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "accuracyMeters" to location.accuracy.takeIf { it > 0 },
            "severity" to SEVERITY_EMERGENCY,
            "incoming" to false,
            "sourceName" to sourceName,
            "message" to message,
            "createdAt" to FieldValue.serverTimestamp(),
            "clearedAt" to null
        )
        runCatching { ref.add(payload).await() }
            .onFailure { Log.w(TAG, "Failed to record outgoing emergency location", it) }
    }

    suspend fun recordIncoming(
        origin: String,
        contact: Contact?,
        location: EmergencyAlertLocation,
        message: String?
    ) {
        if (location.severity != SEVERITY_EMERGENCY) return
        val ref = collectionRef() ?: return
        val payload = hashMapOf(
            "lat" to location.lat,
            "lng" to location.lng,
            "accuracyMeters" to null,
            "severity" to location.severity,
            "incoming" to true,
            "sourceName" to contact?.displayName,
            "sourcePhone" to (contact?.phoneNumber?.takeIf { it.isNotBlank() } ?: origin),
            "sourceContactId" to contact?.id,
            "sourceRemoteUid" to contact?.remoteUid,
            "message" to message,
            "createdAt" to FieldValue.serverTimestamp(),
            "clearedAt" to null
        )
        runCatching { ref.add(payload).await() }
            .onFailure { Log.w(TAG, "Failed to record incoming emergency location", it) }
    }

    fun observeActive(): Flow<List<EmergencyLocation>> = callbackFlow {
        val ref = collectionRef()
        if (ref == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = ref
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Emergency location listener failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val lat = (data["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                    val lng = (data["lng"] as? Number)?.toDouble() ?: return@mapNotNull null
                    val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
                    val clearedAt = (data["clearedAt"] as? Timestamp)?.toDate()?.time
                    EmergencyLocation(
                        id = doc.id,
                        lat = lat,
                        lng = lng,
                        accuracyMeters = (data["accuracyMeters"] as? Number)?.toDouble(),
                        severity = data["severity"] as? String ?: SEVERITY_EMERGENCY,
                        createdAt = createdAt,
                        sourceName = data["sourceName"] as? String,
                        sourcePhone = data["sourcePhone"] as? String,
                        incoming = data["incoming"] as? Boolean ?: true,
                        message = data["message"] as? String,
                        clearedAt = clearedAt
                    )
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun clearLocation(id: String) {
        val ref = collectionRef()?.document(id) ?: return
        runCatching {
            ref.set(mapOf("clearedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        }.onFailure { Log.w(TAG, "Failed to clear emergency location", it) }
    }

    private fun collectionRef() =
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection(COLLECTION)
        }

    companion object {
        private const val TAG = "EmergencyLocationRepo"
        private const val COLLECTION = "emergencyLocations"
        const val SEVERITY_EMERGENCY = "emergency"
    }
}
