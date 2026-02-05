package com.pulselink.data.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.pulselink.domain.model.Contact
import com.pulselink.receiver.SmsSendReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsManager: SmsManager,
    private val smsStore: SmsStore,
    private val smsSyncTrigger: SmsSyncTrigger
) {

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val pendingMessages = ConcurrentHashMap<String, PendingMessage>()

    @RequiresPermission(allOf = [Manifest.permission.SEND_SMS])
    suspend fun sendAlert(
        message: String,
        contacts: List<Contact>,
        delayBetweenMillis: Long = 0L
    ): Int {
        var count = 0
        contacts.forEachIndexed { index, contact ->
            if (sendSms(contact.phoneNumber, message)) {
                count++
            }
            if (delayBetweenMillis > 0 && index < contacts.lastIndex) {
                delay(delayBetweenMillis)
            }
        }
        return count
    }

    @RequiresPermission(Manifest.permission.SEND_SMS)
    suspend fun sendMms(
        phoneNumber: String,
        contentUri: Uri,
        sentIntent: PendingIntent? = null
    ): Boolean {
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
        if (defaultPackage != null && defaultPackage != context.packageName) {
            Log.w(TAG, "Not default SMS app, cannot send MMS")
            return false
        }

        // Grant Telephony stack permission to read the file
        listOf(
            "com.android.mms.service",
            "com.android.providers.telephony",
            context.packageName
        ).forEach { pkg ->
            try {
                context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // best-effort
            }
        }

        val config = Bundle().apply { putString("address", phoneNumber) }

        return runCatching {
            smsManager.sendMultimediaMessage(context, contentUri, null, config, sentIntent)
            true
        }.getOrElse { error ->
            Log.e(TAG, "Failed to send MMS", error)
            false
        }
    }

    @RequiresPermission(Manifest.permission.SEND_SMS)
    suspend fun sendSms(
        phoneNumber: String,
        message: String,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
        awaitResult: Boolean = true
    ): Boolean {
        val timestamp = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString()
        var deferred: CompletableDeferred<Boolean>? = null
        if (awaitResult) {
            deferred = CompletableDeferred()
            pendingRequests[requestId] = deferred
        }

        val sentIntent = Intent(context, SmsSendReceiver::class.java)
            .setAction(ACTION_SMS_SENT)
            .putExtra(EXTRA_REQUEST_ID, requestId)
        val deliveredIntent = Intent(context, SmsSendReceiver::class.java)
            .setAction(ACTION_SMS_DELIVERED)
            .putExtra(EXTRA_REQUEST_ID, requestId)

        val sentPendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.hashCode(),
            sentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deliveredPendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.hashCode(),
            deliveredIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sendResult = runCatching {
            smsManager.sendTextMessage(phoneNumber, null, message, sentPendingIntent, deliveredPendingIntent)
        }.onFailure { error ->
            Log.e(TAG, "Unable to send SMS to $phoneNumber", error)
            pendingRequests.remove(requestId)?.complete(false)
            pendingMessages.remove(requestId)
        }.onSuccess {
            val rowId = smsStore.insertOutgoingPending(phoneNumber, message, timestamp)
            if (rowId != null) {
                pendingMessages[requestId] = PendingMessage(rowId)
            }
        }

        if (!awaitResult) {
            if (sendResult.isFailure) {
                pendingRequests.remove(requestId)
                return false
            }
            return true
        }

        val result = withTimeoutOrNull(timeoutMillis) { deferred?.await() } ?: false
        pendingRequests.remove(requestId)
        return result
    }

    fun handleSendResult(requestId: String, action: String, success: Boolean) {
        val pending = pendingMessages[requestId]
        if (success) {
            when (action) {
                ACTION_SMS_SENT -> {
                    pending?.rowId?.let { smsStore.markOutgoingSent(it) }
                }
                ACTION_SMS_DELIVERED -> pending?.rowId?.let { smsStore.markOutgoingDelivered(it) }
            }
        }
        if (action == ACTION_SMS_SENT) {
            pendingRequests.remove(requestId)?.complete(success)
        }
        if (!success || action == ACTION_SMS_DELIVERED) {
            pendingMessages.remove(requestId)
        }
    }

    private data class PendingMessage(
        val rowId: Long
    )

    companion object {
        private const val TAG = "PulseLinkSmsSender"
        const val ACTION_SMS_SENT = "com.pulselink.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.pulselink.SMS_DELIVERED"
        const val EXTRA_REQUEST_ID = "request_id"
        private const val DEFAULT_TIMEOUT_MS = 30_000L
    }
}
