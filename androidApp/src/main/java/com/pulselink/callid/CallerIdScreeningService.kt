package com.pulselink.callid

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import com.pulselink.data.sms.SmsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Minimal caller ID stub: logs the number and lets the call through.
 * Future work: add spam lookup + on-screen overlay.
 */
@AndroidEntryPoint
class CallerIdScreeningService : CallScreeningService() {

    @Inject lateinit var smsRepository: SmsRepository

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        Log.i(TAG, "Incoming call from $number; letting through.")
        respondToCall(callDetails, CallResponse.Builder()
            .setSilenceCall(false)
            .setDisallowCall(false)
            .build())
    }

    companion object {
        private const val TAG = "CallerIdScreeningSvc"
    }
}
