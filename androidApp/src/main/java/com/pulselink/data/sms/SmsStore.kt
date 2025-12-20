package com.pulselink.data.sms

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists SMS rows into the system Telephony provider. This must only be used
 * while the app holds the default SMS role; otherwise the inserts will fail.
 */
@Singleton
class SmsStore @Inject constructor(
    @ApplicationContext private val context: Context
){
    fun insertIncoming(address: String, body: String, timestamp: Long = System.currentTimeMillis()) {
        if (SmsCodec.isPulseLinkPayload(body)) return
        val values = ContentValues().apply {
            put(Telephony.TextBasedSmsColumns.ADDRESS, address)
            put(Telephony.TextBasedSmsColumns.BODY, body)
            put(Telephony.TextBasedSmsColumns.DATE, timestamp)
            put(Telephony.TextBasedSmsColumns.DATE_SENT, timestamp)
            put(Telephony.TextBasedSmsColumns.READ, 0)
            put(Telephony.TextBasedSmsColumns.SEEN, 0)
            put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX)
            runCatching {
                Telephony.Threads.getOrCreateThreadId(context, setOf(address))
            }.getOrNull()?.let { threadId ->
                put(Telephony.TextBasedSmsColumns.THREAD_ID, threadId)
            }
        }
        runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }.onFailure { error ->
            Log.w(TAG, "Failed to insert incoming SMS into Telephony provider", error)
        }
    }

    fun insertOutgoing(address: String, body: String, timestamp: Long = System.currentTimeMillis()) {
        if (SmsCodec.isPulseLinkPayload(body)) return
        val values = ContentValues().apply {
            put(Telephony.TextBasedSmsColumns.ADDRESS, address)
            put(Telephony.TextBasedSmsColumns.BODY, body)
            put(Telephony.TextBasedSmsColumns.DATE, timestamp)
            put(Telephony.TextBasedSmsColumns.DATE_SENT, timestamp)
            put(Telephony.TextBasedSmsColumns.READ, 1)
            put(Telephony.TextBasedSmsColumns.SEEN, 1)
            put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT)
            put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_COMPLETE)
            runCatching {
                Telephony.Threads.getOrCreateThreadId(context, setOf(address))
            }.getOrNull()?.let { threadId ->
                put(Telephony.TextBasedSmsColumns.THREAD_ID, threadId)
            }
        }
        runCatching {
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
        }.onFailure { error ->
            Log.w(TAG, "Failed to insert outgoing SMS into Telephony provider", error)
        }
    }

    companion object {
        private const val TAG = "SmsStore"
    }
}
