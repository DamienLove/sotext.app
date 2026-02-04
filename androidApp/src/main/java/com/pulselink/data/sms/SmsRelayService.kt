package com.pulselink.data.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRelayService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val smsSender: SmsSender,
    private val settingsRepository: SettingsRepository,
    private val smsSyncTrigger: SmsSyncTrigger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var outboxListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private val isStarted = AtomicBoolean(false)
    private var lastSyncRequestedAt: com.google.firebase.Timestamp? = null
    private var isFirstSnapshot = true

    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                startListening(user.uid)
            } else {
                stopListening()
            }
        }
    }

    private fun startListening(uid: String) {
        if (outboxListener == null) {
            val outboxRef = firestore.collection("users").document(uid).collection("outbox")
            outboxListener = outboxRef.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Outbox listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (doc in snapshots.documents) {
                        val data = doc.data ?: continue
                        val address = data["address"] as? String
                        val body = data["body"] as? String
                        val lineId = data["lineId"] as? String
                        val status = data["status"] as? String
                        val attachmentUrl = data["attachmentUrl"] as? String

                        // Skip messages that have already been processed or failed
                        if (status == "failed" || status == "sent") continue

                        if (address != null && (body != null || attachmentUrl != null)) {
                            processMessage(doc.id, address, body ?: "", uid, lineId, attachmentUrl)
                        }
                    }
                }
            }
        }

        if (userListener == null) {
            isFirstSnapshot = true
            val userRef = firestore.collection("users").document(uid)
            userListener = userRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "User listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val syncRequestedAt = snapshot.getTimestamp("syncRequestedAt")
                    val remoteWebAccess = snapshot.getBoolean("remoteWebAccessEnabled")

                    if (remoteWebAccess != null) {
                        scope.launch {
                            try {
                                val currentSettings = settingsRepository.settings.first()
                                if (currentSettings.remoteWebAccessEnabled != remoteWebAccess) {
                                    Log.d(TAG, "Syncing remoteWebAccessEnabled from cloud: $remoteWebAccess")
                                    settingsRepository.setRemoteWebAccessEnabled(remoteWebAccess)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to sync remote settings", e)
                            }
                        }
                    }

                    if (isFirstSnapshot) {
                        isFirstSnapshot = false
                        lastSyncRequestedAt = syncRequestedAt
                    } else if (syncRequestedAt != null && syncRequestedAt != lastSyncRequestedAt) {
                        // Timestamp changed, trigger sync
                        Log.d(TAG, "Sync requested from web at $syncRequestedAt")
                        lastSyncRequestedAt = syncRequestedAt
                        smsSyncTrigger.triggerSync()
                    }
                }
            }
        }
    }

    private fun stopListening() {
        outboxListener?.remove()
        outboxListener = null
        userListener?.remove()
        userListener = null
        lastSyncRequestedAt = null
        isFirstSnapshot = true
    }

    private fun processMessage(
        docId: String,
        address: String,
        body: String,
        uid: String,
        lineId: String?,
        attachmentUrl: String?
    ) {
        scope.launch {
            try {
                val settings = settingsRepository.settings.first()
                if (!settings.remoteWebAccessEnabled) {
                    return@launch
                }

                val deviceId = settingsRepository.ensureDeviceId()
                if (!lineId.isNullOrBlank() && lineId != deviceId) {
                    return@launch
                }

                val success = if (attachmentUrl != null) {
                    downloadAndSendMms(address, attachmentUrl)
                } else {
                    smsSender.sendSms(address, body)
                }

                if (success) {
                    firestore.collection("users").document(uid)
                        .collection("outbox").document(docId).delete()
                } else {
                    Log.w(TAG, "Failed to send SMS via relay to $address")
                    // Mark as failed. The web client should listen for this status and handle cleanup or retry.
                    firestore.collection("users").document(uid)
                        .collection("outbox").document(docId)
                        .update("status", "failed", "error", "SMS dispatch failed")
                }

                val status = if (success) "sent" else "failed"
                writeDiagnostics(uid, deviceId, address, status)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during SMS relay", e)
            }
        }
    }

    private suspend fun downloadAndSendMms(address: String, urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            // Use cache dir for temp file
            val file = File(context.cacheDir, "mms_${UUID.randomUUID()}.tmp")

            withContext(Dispatchers.IO) {
                url.openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Must match authorities in AndroidManifest.xml: ${applicationId}.export
            // NOTE: file_paths.xml usually points to files/exports, not cache.
            // Let's check where SmsIntents writes. It writes to context.filesDir/exports.
            // We should stick to that if file_paths.xml expects it.
            // Let's move the file or write it there directly.

            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val destFile = File(exportDir, file.name)
            file.copyTo(destFile, overwrite = true)
            file.delete()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.export", destFile)

            val cleanupIntent = Intent(context, MmsSentReceiver::class.java).apply {
                putExtra(MmsSentReceiver.EXTRA_FILE_PATH, destFile.absolutePath)
            }

            // Unique request code to avoid collision
            val requestCode = destFile.name.hashCode()

            val pendingCleanup = PendingIntent.getBroadcast(
                context,
                requestCode,
                cleanupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val result = smsSender.sendMms(address, uri, pendingCleanup)
            // cleanup is handled by MmsSentReceiver
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download/send MMS", e)
            false
        }
    }

    private suspend fun writeDiagnostics(
        uid: String,
        deviceId: String,
        address: String,
        status: String
    ) {
        try {
            val doc = firestore.collection("users").document(uid)
                .collection("relayDiagnostics")
                .document("latest")

            val payload = mapOf(
                "deviceId" to deviceId,
                "recipient" to address,
                "status" to status,
                "timestamp" to FieldValue.serverTimestamp()
            )
            doc.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write relay diagnostics", e)
        }
    }

    companion object {
        private const val TAG = "SmsRelayService"
    }
}
