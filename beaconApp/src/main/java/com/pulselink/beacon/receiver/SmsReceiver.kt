package com.pulselink.beacon.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.pulselink.beacon.data.InboxPreferencesRepository
import com.pulselink.beacon.data.MessageNotificationPreferences
import com.pulselink.beacon.data.SmsSyncManager
import com.pulselink.beacon.data.SmsRepository
import com.pulselink.beacon.data.SotextProtocol
import com.pulselink.beacon.notifications.MessageNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receives inbound SMS when Beacon is set as the default SMS app.
 * Persists the message into the system Telephony provider so it
 * becomes visible to the inbox/thread UI.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val origin = msgs.firstOrNull()?.displayOriginatingAddress.orEmpty()
                val body = msgs.joinToString(separator = "") { it.displayMessageBody }.trim()

                // Intercept delete commands before writing to inbox
                val deleteCommand = SotextProtocol.parseDeleteCommand(body)
                if (deleteCommand != null && origin.isNotBlank()) {
                    handleDeleteCommand(context, origin, deleteCommand)
                    return@launch
                }

                msgs.forEach { sms ->
                    writeToInbox(context, sms)
                }
                if (origin.isNotBlank() && body.isNotBlank()) {
                    val timestamp = msgs.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()

                    runCatching {
                        SmsSyncManager.getInstance(context).syncIncoming(origin, body, timestamp)
                    }

                    // Auto Reply Logic
                    handleAutoReply(context, origin, body)

                    val settings = MessageNotificationPreferences(context).getConfig()
                    val threadId = runCatching {
                        Telephony.Threads.getOrCreateThreadId(context, origin)
                    }.getOrNull()
                    MessageNotificationManager.notifyIncoming(
                        context = context,
                        address = origin,
                        body = body,
                        timestamp = timestamp,
                        settings = settings,
                        threadId = threadId
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun writeToInbox(context: Context, sms: SmsMessage) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sms.displayOriginatingAddress)
            put(Telephony.Sms.BODY, sms.displayMessageBody)
            put(Telephony.Sms.DATE, sms.timestampMillis)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val threadId = Telephony.Threads.getOrCreateThreadId(
                    context,
                    sms.displayOriginatingAddress
                )
                put(Telephony.Sms.THREAD_ID, threadId)
            }
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
    }

    private fun handleDeleteCommand(
        context: Context,
        origin: String,
        command: SotextProtocol.DeleteCommand
    ) {
        val lowerBound = command.timestamp - 10_000L
        val upperBound = command.timestamp + 10_000L
        val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY)
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                "${Telephony.Sms.ADDRESS}=? AND ${Telephony.Sms.DATE} BETWEEN ? AND ?",
                arrayOf(origin, lowerBound.toString(), upperBound.toString()),
                null
            )
        }.getOrNull() ?: return

        val idsToDelete = mutableListOf<Long>()
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            while (c.moveToNext()) {
                val msgBody = c.getString(bodyIdx) ?: ""
                val snippetMatches = command.bodySnippet.isBlank() ||
                    msgBody.startsWith(command.bodySnippet) ||
                    (msgBody.length < command.bodySnippet.length && command.bodySnippet.startsWith(msgBody))
                if (snippetMatches) {
                    idsToDelete += c.getLong(idIdx)
                }
            }
        }

        val repo = SmsRepository(context)
        idsToDelete.forEach { id -> repo.deleteMessage(id) }
    }

    private suspend fun handleAutoReply(context: Context, origin: String, body: String) {
        val prefs = InboxPreferencesRepository(context)
        val state = prefs.flow.first()
        if (state.autoReplyEnabled && state.autoReplyMessage.isNotBlank()) {
             // 1. Basic filtering: Don't reply to short codes (length < 6) or if it looks like OTP
             // 2. Don't reply to self (unlikely here)
             // 3. Prevent loop: Don't reply if the incoming message is exactly the auto-reply message (simple check)

             val isShortCode = origin.filter { it.isDigit() }.length < 7
             if (isShortCode) return

             if (body == state.autoReplyMessage) return // Loop prevention

             // Check for OTP keywords to avoid replying to automated systems
             val otpKeywords = listOf("code", "otp", "verification", "password", "login")
             if (otpKeywords.any { body.contains(it, ignoreCase = true) }) return

             // Send Reply
             val repo = SmsRepository(context)
             repo.sendSms(origin, state.autoReplyMessage)
        }
    }
}
