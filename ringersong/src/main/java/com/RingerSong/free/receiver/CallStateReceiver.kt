package com.RingerSong.free.receiver

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.RingerSong.free.data.AppStateStore
import com.RingerSong.free.service.RingerPlaybackService
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

        when (phoneState) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d(TAG, "RINGING from: $number - Starting RingerPlaybackService")

                // Start the playback service immediately to silence default ringer and play stream
                val serviceIntent = Intent(context, RingerPlaybackService::class.java).apply {
                    action = RingerPlaybackService.ACTION_PLAY_SEGMENT
                    putExtra(RingerPlaybackService.EXTRA_PHONE_NUMBER, number)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service (likely Android 12+ background restriction)", e)
                    // Fallback or retry logic could go here, but for now we log and proceed safely.
                    // If we can't start the service, we can't play streaming audio.
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended or rejected
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING ||
                    lastState == TelephonyManager.EXTRA_STATE_OFFHOOK) {

                    Log.d(TAG, "Call ended - Stopping RingerPlaybackService")
                    val serviceIntent = Intent(context, RingerPlaybackService::class.java).apply {
                        action = RingerPlaybackService.ACTION_STOP_PLAYBACK
                    }
                    try {
                        context.startService(serviceIntent)
                    } catch (e: Exception) {
                         Log.e(TAG, "Failed to send STOP intent", e)
                    }

                    // We still keep the "Next Ringtone" logic for fallback/LOCAL support if needed,
                    // but for Streaming, the service handles it.
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val store = AppStateStore(context)
                            val ringtoneManager = RingtoneSegmentManager(context, store)

                            // Check if enabled
                            val state = store.stateFlow.first()
                            if (!state.settings.enabled) {
                                pending.finish()
                                return@launch
                            }

                            // Set the NEXT ringtone (for the next incoming call) - mostly relevant for LOCAL files
                            ringtoneManager.setRingtoneForIncomingCall(null)

                            // Cleanup old segments
                            ringtoneManager.cleanupOldSegments()
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // User answered the call. Stop ringing.
                Log.d(TAG, "Call Answered - Stopping RingerPlaybackService")
                val serviceIntent = Intent(context, RingerPlaybackService::class.java).apply {
                    action = RingerPlaybackService.ACTION_STOP_PLAYBACK
                }
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                     Log.e(TAG, "Failed to send STOP intent", e)
                }
            }
        }

        lastState = phoneState
    }
}
