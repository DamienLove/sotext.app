package com.pulselink.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.pulselink.service.AlertRouter

/**
 * Minimal WAP push receiver so Pro/Premium can satisfy default SMS role requirements.
 * Currently we just log and forward the raw payload to the alert router for visibility.
 */
@AndroidEntryPoint
class MmsWapPushReceiver : BroadcastReceiver() {

    @Inject lateinit var alertRouter: AlertRouter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.WAP_PUSH_DELIVER") return
        val data = intent.getByteArrayExtra("data")
        Log.i(TAG, "Received WAP_PUSH_DELIVER MMS payload=${data?.size ?: 0} bytes")
        runCatching {
            alertRouter.onInboundMessage("[MMS] Received ${data?.size ?: 0} bytes")
        }.onFailure { error ->
            Log.w(TAG, "Unable to route MMS payload", error)
        }
    }

    companion object {
        private const val TAG = "PulseLinkMmsReceiver"
    }
}
