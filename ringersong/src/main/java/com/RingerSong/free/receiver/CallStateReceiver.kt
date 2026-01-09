package com.RingerSong.free.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.service.RingtoneSegmentManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallStateReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallStateReceiver"
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        Log.d(TAG, "Phone state: $phoneState (last: $lastState)")

        // Set the NEXT ringtone when call ends (not during ringing - too late!)
        when (phoneState) {
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended - set the next ringtone for the NEXT call
                if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK ||
                    lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                    Log.d(TAG, "Call ended - setting NEXT ringtone")

                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val store = AppStateStore(context)
                            val ringtoneManager = RingtoneSegmentManager(context, store)

                            // Check if enabled
                            val state = store.stateFlow.first()
                            if (!state.settings.enabled) {
                                Log.w(TAG, "RingerSong is disabled")
                                pending.finish()
                                return@launch
                            }

                            // Set the NEXT ringtone (for the next incoming call)
                            ringtoneManager.setRingtoneForIncomingCall(null)

                            // Cleanup old segments
                            ringtoneManager.cleanupOldSegments()
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d(TAG, "RINGING from: $number (ringtone already set)")
                // The ringtone is already set from the previous call or initial setup
                // Just log that we received the call
            }
        }

        lastState = phoneState
    }
}
