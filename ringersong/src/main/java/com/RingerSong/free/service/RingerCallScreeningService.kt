package com.RingerSong.free.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.RingerSong.free.data.AppStateStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RingerCallScreeningService : CallScreeningService() {

    @Inject lateinit var appStateStore: AppStateStore

    companion object {
        private const val TAG = "RingerCallScreening"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        // Use schemeSpecificPart to get the raw number
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        Log.d(TAG, "Screening call from: $phoneNumber")

        // Check incoming direction to be safe (though CallScreening mostly handles incoming)
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
             respondToCall(callDetails, CallResponse.Builder().build())
             return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check if feature is enabled
                val state = appStateStore.stateFlow.first()
                if (state.settings.enabled) {
                    Log.d(TAG, "RingerSong enabled. Silencing system ringer.")

                    // Silence the call using the API
                    val response = CallResponse.Builder()
                        .setSilenceCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build()

                    respondToCall(callDetails, response)

                    // Start our custom playback
                    startPlaybackService(phoneNumber)

                } else {
                    Log.d(TAG, "RingerSong disabled. Allowing call.")
                    respondToCall(callDetails, CallResponse.Builder().build())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking AppState", e)
                // Fallback: allow call
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private fun startPlaybackService(phoneNumber: String?) {
        try {
            val serviceIntent = Intent(this, RingerPlaybackService::class.java).apply {
                action = RingerPlaybackService.ACTION_PLAY_SEGMENT
                putExtra(RingerPlaybackService.EXTRA_PHONE_NUMBER, phoneNumber)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "Started RingerPlaybackService")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback service", e)
        }
    }
}
