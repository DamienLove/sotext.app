package com.pulselink.beacon.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import com.pulselink.beacon.data.SmsRepository

/**
 * Handles quick-reply intents (e.g., from dialer) when Beacon is the
 * default SMS app.
 */
class RespondViaMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (TelephonyManager.ACTION_RESPOND_VIA_MESSAGE != intent.action) return
        val uri: Uri? = intent.data
        val address = uri?.schemeSpecificPart ?: return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        SmsRepository(context).sendSms(address, text)
    }
}
