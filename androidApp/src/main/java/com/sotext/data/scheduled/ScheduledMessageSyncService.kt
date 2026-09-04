package com.sotext.data.scheduled

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.sotext.domain.model.RecurrenceFrequency
import com.sotext.domain.model.RecurrenceRule
import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.model.ScheduledMessageStatus
import com.sotext.domain.repository.ScheduledMessageRepository
import com.sotext.domain.repository.SettingsRepository
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Cross-device/web sync for Scheduled Messages, Premium-only (gated by `remoteWebAccessEnabled`),
 * sibling to [com.sotext.data.sms.SmsRelayService] and deliberately shaped the same way: an auth
 * listener that starts/stops a Firestore snapshot listener, and a settings-driven on/off switch.
 *
 * Room is always the source of truth for what actually fires a send - this service only mirrors
 * state so the web portal and other devices can see/edit it. **Never sends anything itself.**
 *
 * Conflict policy: last-write-wins on `updatedAt`, mirroring the outbox's own implicit LWW.
 */
@Singleton
class ScheduledMessageSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val settingsRepository: SettingsRepository,
    private val scheduledMessageRepository: ScheduledMessageRepository,
    private val alarmScheduler: ScheduledMessageAlarmScheduler,
    private val dispatcher: ScheduledMessageDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firestoreListener: ListenerRegistration? = null
    private val isStarted = AtomicBoolean(false)
    private var currentUid: String? = null

    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            currentUid = uid
            if (uid == null) {
                stopListening()
            } else {
                scope.launch { reconcileEnabledState(uid) }
            }
        }

        // React live to the setting being toggled, not just at sign-in.
        settingsRepository.settings
            .map { it.remoteWebAccessEnabled }
            .distinctUntilChanged()
            .onEach { enabled ->
                val uid = currentUid ?: return@onEach
                if (enabled) startListening(uid) else stopListening()
            }
            .launchIn(scope)
    }

    private suspend fun reconcileEnabledState(uid: String) {
        if (settingsRepository.settings.first().remoteWebAccessEnabled) {
            startListening(uid)
        } else {
            stopListening()
        }
        pushLocalChanges(uid)
    }

    private fun startListening(uid: String) {
        if (firestoreListener != null) return
        val collection = firestore.collection("users").document(uid).collection("scheduledMessages")
        firestoreListener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Scheduled-message listen failed", error)
                return@addSnapshotListener
            }
            val docs = snapshot?.documents ?: return@addSnapshotListener
            scope.launch { docs.forEach { applyRemoteDoc(it.id, it.data ?: return@forEach) } }
        }
        scope.launch { pushLocalChanges(uid) }
    }

    private fun stopListening() {
        firestoreListener?.remove()
        firestoreListener = null
    }

    /** Uploads every locally-dirty row (syncedToCloud == false) - new schedules, edits, and status transitions alike. */
    private suspend fun pushLocalChanges(uid: String) {
        val deviceId = settingsRepository.ensureDeviceId()
        val collection = firestore.collection("users").document(uid).collection("scheduledMessages")
        scheduledMessageRepository.getUnsyncedToCloud().forEach { message ->
            try {
                collection.document(message.occurrenceKey).set(toFirestorePayload(message, deviceId), SetOptions.merge())
                scheduledMessageRepository.markSyncedToCloud(message.id, message.occurrenceKey)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push scheduled message ${message.occurrenceKey}", e)
            }
        }
    }

    private suspend fun applyRemoteDoc(occurrenceKey: String, data: Map<String, Any?>) {
        val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: return
        val local = scheduledMessageRepository.getByOccurrenceKey(occurrenceKey)

        // LWW: a doc no newer than what we already have (including one we just pushed ourselves,
        // whose local updatedAt now matches) needs no action.
        if (local != null && remoteUpdatedAt <= local.updatedAt) return

        val remoteStatus = data["status"] as? String
        if (remoteStatus == SEND_NOW_SENTINEL) {
            // Web's "Send now" write: not a real status, a request to bypass the alarm and
            // dispatch immediately. The doc's actual status field is left as SCHEDULED/FAILED by
            // the web client - only this sentinel value changes - so the local row's real status
            // is untouched here; the dispatch itself will move it to PROCESSING/SENT/FAILED, which
            // then syncs back up normally.
            if (local != null) {
                alarmScheduler.cancel(local.id)
                dispatcher.sendNowManually(local.id)
            }
            return
        }

        val status = remoteStatus?.let {
            runCatching { ScheduledMessageStatus.valueOf(it) }.getOrNull()
        } ?: ScheduledMessageStatus.SCHEDULED
        val recurrenceMap = data["recurrenceRule"] as? Map<*, *>
        val recurrenceRule = recurrenceMap?.let { toRecurrenceRule(it) }

        val merged = (local ?: ScheduledMessage(
            occurrenceKey = occurrenceKey,
            address = data["address"] as? String ?: return,
            body = "",
            scheduledForUtcMillis = 0L,
            timezoneId = java.time.ZoneId.systemDefault().id
        )).copy(
            seriesId = data["seriesId"] as? String ?: local?.seriesId ?: UUID.randomUUID().toString(),
            threadId = (data["threadId"] as? Number)?.toLong() ?: local?.threadId,
            address = data["address"] as? String ?: local?.address.orEmpty(),
            body = data["body"] as? String ?: local?.body.orEmpty(),
            // Attachments are not synced in v1 (they'd need a Storage upload/download round-trip
            // this pass doesn't add) - a remote edit never touches the local device's own copies.
            lineId = data["lineId"] as? String ?: local?.lineId,
            scheduledForUtcMillis = (data["scheduledForUtcMillis"] as? Number)?.toLong() ?: local?.scheduledForUtcMillis ?: 0L,
            timezoneId = data["timezoneId"] as? String ?: local?.timezoneId ?: java.time.ZoneId.systemDefault().id,
            recurrenceRule = recurrenceRule,
            status = status,
            retryCount = (data["retryCount"] as? Number)?.toInt() ?: local?.retryCount ?: 0,
            lastError = data["lastError"] as? String,
            updatedAt = remoteUpdatedAt,
            syncedToCloud = true,
            cloudDocId = occurrenceKey
        )

        if (local == null) {
            val id = scheduledMessageRepository.insert(merged)
            reconcileAlarm(merged.copy(id = id))
        } else {
            scheduledMessageRepository.update(merged.copy(id = local.id, syncedToCloud = true))
            reconcileAlarm(merged.copy(id = local.id))
        }
    }

    private fun reconcileAlarm(message: ScheduledMessage) {
        when (message.status) {
            ScheduledMessageStatus.SCHEDULED -> alarmScheduler.scheduleExact(message)
            else -> alarmScheduler.cancel(message.id)
        }
    }

    private fun toFirestorePayload(message: ScheduledMessage, deviceId: String): Map<String, Any?> = mapOf(
        "seriesId" to message.seriesId,
        "threadId" to message.threadId,
        "address" to message.address,
        "body" to message.body,
        "attachmentUrls" to emptyList<String>(),
        "lineId" to message.lineId,
        "scheduledForUtcMillis" to message.scheduledForUtcMillis,
        "timezoneId" to message.timezoneId,
        "recurrenceRule" to message.recurrenceRule?.let {
            mapOf(
                "frequency" to it.frequency.name,
                "interval" to it.interval,
                "daysOfWeek" to it.daysOfWeek.toList(),
                "dayOfMonth" to it.dayOfMonth,
                "endDateUtcMillis" to it.endDateUtcMillis,
                "occurrenceCount" to it.occurrenceCount
            )
        },
        "status" to message.status.name,
        "retryCount" to message.retryCount,
        "lastError" to message.lastError,
        "createdAt" to message.createdAt,
        "updatedAt" to message.updatedAt,
        "fromDeviceId" to deviceId
    )

    private fun toRecurrenceRule(map: Map<*, *>): RecurrenceRule? {
        val frequency = (map["frequency"] as? String)?.let {
            runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull()
        } ?: return null
        @Suppress("UNCHECKED_CAST")
        val daysOfWeek = (map["daysOfWeek"] as? List<Number>)?.map { it.toInt() }?.toSet() ?: emptySet()
        return RecurrenceRule(
            frequency = frequency,
            interval = (map["interval"] as? Number)?.toInt() ?: 1,
            daysOfWeek = daysOfWeek,
            dayOfMonth = (map["dayOfMonth"] as? Number)?.toInt(),
            endDateUtcMillis = (map["endDateUtcMillis"] as? Number)?.toLong(),
            occurrenceCount = (map["occurrenceCount"] as? Number)?.toInt()
        )
    }

    companion object {
        private const val TAG = "ScheduledMsgSync"
        /** Web-portal "Send now" writes this instead of a real status - see [applyRemoteDoc]. */
        const val SEND_NOW_SENTINEL = "SEND_NOW_REQUESTED"
    }
}
