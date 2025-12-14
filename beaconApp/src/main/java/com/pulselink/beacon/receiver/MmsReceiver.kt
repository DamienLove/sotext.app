package com.pulselink.beacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony

/**
 * Minimal MMS/WAP push receiver. For now we just acknowledge receipt so
 * the platform can download the MMS using the system stack.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION == intent.action ||
            Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION == intent.action
        ) {
            // Rely on platform download; nothing to persist yet.
            resultCode = android.app.Activity.RESULT_OK
        }
    }
}
