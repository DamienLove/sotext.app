package com.pulselink.data.link

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.data.sms.PulseLinkMessage
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class LinkChannelService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val authManager: FirebaseAuthManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val listeners = mutableMapOf<String, ListenerRegistration>()
    private val _inboundMessages = MutableSharedFlow<LinkChannelPayload>(extraBufferCapacity = 64)
    val inboundMessages: SharedFlow<LinkChannelPayload> = _inboundMessages.asSharedFlow()
    @Volatile private var localDeviceId: String? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            // Attempt to ensure authentication, but don't block service startup if it fails
            runCatching {
                authManager.ensureSignedIn()
            }.onFailure {
                Log.w(TAG, "Failed to authenticate on LinkChannelService start, service will continue with limited functionality", it)
            }
            val deviceId = settingsRepository.ensureDeviceId()
            localDeviceId = deviceId
            contactRepository.observeContacts().collect { contacts ->
                syncListeners(deviceId, contacts)
            }
        }
    }

    suspend fun sendMessage(message: PulseLinkMessage): Boolean {
        start()

        // Attempt authentication, but don't fail the entire message send if auth fails
        // This allows SMS fallback to work even when Firebase auth is unavailable
        val isAuthenticated = runCatching {
            authManager.ensureSignedIn()
            true
        }.onFailure {
            Log.w(TAG, "Authentication failed during message send, will rely on fallback channels", it)
        }.getOrDefault(false)

        if (!isAuthenticated) {
            // Skip Firebase channel if authentication failed
            return false
        }

        // We need to find the remote device ID. Ideally, the message should have it, or we look it up.
        // PulseLinkMessage has senderId (which is us) and code.
        // We need to resolve the receiver from the code.

        val contact = contactRepository.getByLinkCode(message.code)
        val remoteDeviceId = contact?.remoteDeviceId
            ?: return false // Cannot send without remoteDeviceId

        val senderId = localDeviceId ?: settingsRepository.ensureDeviceId().also { localDeviceId = it }
        val channelId = channelIdFor(senderId, remoteDeviceId)

        val payload = hashMapOf<String, Any>(
            FIELD_ID to UUID.randomUUID().toString(),
            FIELD_SENDER_ID to senderId,
            FIELD_RECEIVER_ID to remoteDeviceId,
            FIELD_TIMESTAMP to System.currentTimeMillis(),
            FIELD_STATUS to "SENT"
        )

        // Add message specific fields
        when (message) {
            is PulseLinkMessage.LinkRequest -> {
                payload[FIELD_TYPE] = TYPE_LINK_REQUEST
                payload["linkCode"] = message.code
                payload["senderName"] = message.senderName
            }
            is PulseLinkMessage.LinkAccept -> {
                payload[FIELD_TYPE] = TYPE_LINK_ACCEPT
                payload["linkCode"] = message.code
            }
            is PulseLinkMessage.Ping -> {
                payload[FIELD_TYPE] = TYPE_PING
                payload["linkCode"] = message.code
            }
            is PulseLinkMessage.RemoteAlert -> {
                payload[FIELD_TYPE] = TYPE_ALERT
                payload["linkCode"] = message.code
                payload["tier"] = message.tier.name
            }
            is PulseLinkMessage.AlertPrepare -> {
                payload[FIELD_TYPE] = TYPE_ALERT_PREPARE
                payload["linkCode"] = message.code
                payload["tier"] = message.tier.name
                payload["reason"] = message.reason.name
            }
            is PulseLinkMessage.AlertReady -> {
                payload[FIELD_TYPE] = TYPE_ALERT_READY
                payload["linkCode"] = message.code
                payload["ready"] = message.ready
            }
            is PulseLinkMessage.SoundOverride -> {
                payload[FIELD_TYPE] = TYPE_SOUND_OVERRIDE
                payload["linkCode"] = message.code
                payload["tier"] = message.tier.name
                payload["soundKey"] = message.soundKey.orEmpty()
            }
            is PulseLinkMessage.ManualMessage -> {
                payload[FIELD_TYPE] = TYPE_MANUAL
                payload["linkCode"] = message.code
                payload["body"] = message.body
                payload["urgency"] = message.urgency.name
                payload["volumeHint"] = message.volumeHint?.name ?: ""
            }
            is PulseLinkMessage.ConfigUpdate -> {
                payload[FIELD_TYPE] = TYPE_CONFIG
                payload["linkCode"] = message.code
                payload["key"] = message.key
                payload["value"] = message.value
            }
            is PulseLinkMessage.CallEnded -> {
                payload[FIELD_TYPE] = TYPE_CALL_ENDED
                payload["linkCode"] = message.code
                payload["callDuration"] = message.callDuration
            }
        }

        // Add phone number if available for backward compatibility or extra info
        contact.phoneNumber.takeIf { it.isNotBlank() }?.let { payload[FIELD_PHONE] = it }

        return runCatching {
            firestore.collection(COLLECTION_CHANNELS)
                .document(channelId)
                .collection(COLLECTION_MESSAGES)
                .document(payload[FIELD_ID] as String)
                .set(payload)
                .await()
        }.onFailure {
            Log.w(TAG, "Failed to send message via $channelId", it)
        }.isSuccess
    }

    // Deprecated: Kept for compatibility if needed, but redirects to sendMessage
    suspend fun sendManualMessage(
        contact: Contact,
        body: String,
        urgency: com.pulselink.domain.model.MessageUrgency,
        volumeHint: com.pulselink.domain.model.VolumeHint?
    ): Boolean {
        val linkCode = contact.linkCode ?: return false
        val deviceId = localDeviceId ?: settingsRepository.ensureDeviceId()
        val message = PulseLinkMessage.ManualMessage(
            senderId = deviceId,
            code = linkCode,
            body = body,
            urgency = urgency,
            volumeHint = volumeHint
        )
        return sendMessage(message)
    }

    private fun syncListeners(localId: String, contacts: List<Contact>) {
        val desired = contacts
            .filter { it.linkStatus == LinkStatus.LINKED && !it.remoteDeviceId.isNullOrBlank() }
            .associate { contact ->
                val channelId = channelIdFor(localId, contact.remoteDeviceId!!)
                channelId to contact.id
            }

        val stale = listeners.keys - desired.keys
        stale.forEach { channelId ->
            listeners.remove(channelId)?.remove()
        }

        desired.forEach { (channelId, contactId) ->
            if (!listeners.containsKey(channelId)) {
                attachListener(channelId, contactId, localId)
            }
        }
    }

    private fun attachListener(channelId: String, contactId: Long, localId: String) {
        val registration = firestore.collection(COLLECTION_CHANNELS)
            .document(channelId)
            .collection(COLLECTION_MESSAGES)
            .whereEqualTo(FIELD_RECEIVER_ID, localId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w(TAG, "Listener error for $channelId", error)
                    return@addSnapshotListener
                }
                if (snapshots == null || snapshots.isEmpty) return@addSnapshotListener
                for (change in snapshots.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val type = doc.getString(FIELD_TYPE) ?: TYPE_MANUAL
                        val payloadMap = doc.data

                        val payload = LinkChannelPayload(
                            id = doc.getString(FIELD_ID).orEmpty(),
                            senderId = doc.getString(FIELD_SENDER_ID).orEmpty(),
                            receiverId = doc.getString(FIELD_RECEIVER_ID).orEmpty(),
                            timestamp = doc.getLong(FIELD_TIMESTAMP) ?: System.currentTimeMillis(),
                            type = type,
                            payload = payloadMap
                        )
                        scope.launch {
                            _inboundMessages.emit(payload)
                            doc.reference.delete()
                        }
                    }
                }
            }
        listeners[channelId] = registration
    }

    private fun channelIdFor(a: String, b: String): String =
        listOf(a, b).sorted().joinToString("_")

    companion object {
        private const val TAG = "LinkChannelService"
        private const val COLLECTION_CHANNELS = "linkChannels"
        private const val COLLECTION_MESSAGES = "messages"
        private const val FIELD_ID = "id"
        private const val FIELD_SENDER_ID = "senderId"
        private const val FIELD_RECEIVER_ID = "receiverId"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_TYPE = "type"
        private const val FIELD_PHONE = "phoneNumber"
        private const val FIELD_STATUS = "status"

        // Message Types
        private const val TYPE_MANUAL = "manual"
        private const val TYPE_LINK_REQUEST = "link_request"
        private const val TYPE_LINK_ACCEPT = "link_accept"
        private const val TYPE_PING = "ping"
        private const val TYPE_ALERT = "alert"
        private const val TYPE_ALERT_PREPARE = "alert_prepare"
        private const val TYPE_ALERT_READY = "alert_ready"
        private const val TYPE_SOUND_OVERRIDE = "sound_override"
        private const val TYPE_CONFIG = "config"
        private const val TYPE_CALL_ENDED = "call_ended"
    }
}

data class LinkChannelPayload(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val timestamp: Long,
    val type: String,
    val payload: Map<String, Any?>
)
