package com.pulselink.beacon.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage

/**
 * Receives inbound SMS when Beacon is set as the default SMS app.
 * Persists the message into the system Telephony provider so it
 * becomes visible to the inbox/thread UI.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        msgs.forEach { sms ->
            writeToInbox(context, sms)
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
}
