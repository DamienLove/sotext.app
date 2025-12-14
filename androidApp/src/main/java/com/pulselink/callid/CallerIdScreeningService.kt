package com.pulselink.callid

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Minimal caller ID stub: logs the number and lets the call through.
 * Future work: add spam lookup + on-screen overlay.
 */
@AndroidEntryPoint
class CallerIdScreeningService : CallScreeningService() {

    @Inject lateinit var numLookupApiClient: NumLookupApiClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        if (number.isBlank()) {
            respondToCall(callDetails, allowResponse())
            return
        }

        serviceScope.launch {
            val lookup = withTimeoutOrNull(3500) {
                numLookupApiClient.lookup(number)
            }
            val shouldSilence = lookup?.isLikelySpam == true
            Log.i(TAG, "Incoming call from $number; lookup=${lookup?.summary ?: "none"} silence=$shouldSilence")
            respondToCall(callDetails, CallResponse.Builder()
                .setSilenceCall(shouldSilence)
                .setDisallowCall(false)
                .build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun allowResponse(): CallResponse =
        CallResponse.Builder()
            .setSilenceCall(false)
            .setDisallowCall(false)
            .build()

    companion object {
        private const val TAG = "CallerIdScreeningSvc"
    }
}
