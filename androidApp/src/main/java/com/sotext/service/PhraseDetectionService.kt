package com.sotext.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

/**
 * Placeholder for passive, always-on voice-activated safety-phrase detection
 * (see [AlertRouter.onPhraseDetected] and [com.sotext.domain.model.PulseLinkSettings.phrases]).
 *
 * Not implemented yet. A real attempt using [android.speech.SpeechRecognizer] in a continuous
 * restart loop was built and tested on-device; it surfaced two problems worth noting before
 * trying again:
 *  - Offline recognition fails with ERROR_LANGUAGE_UNAVAILABLE on devices without a downloaded
 *    on-device language pack, and framework fallback to network recognition is inconsistent
 *    across recognizer implementations (observed both engines attempting to start concurrently
 *    on one test device).
 *  - No UI currently exposes this to users — it needs a real onboarding/consent flow, a way to
 *    set a custom phrase, and a decision on a dedicated wake-word engine vs. plain
 *    SpeechRecognizer before it's worth shipping.
 *
 * This stub exists so the manifest declaration, permissions (RECORD_AUDIO,
 * FOREGROUND_SERVICE_MICROPHONE), and the AlertRouter hook stay ready for that future work.
 * Nothing currently starts this service.
 */
@AndroidEntryPoint
class PhraseDetectionService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Passive phrase detection is not implemented yet; stopping.")
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PhraseDetectionSvc"

        fun newIntent(context: Context) = Intent(context, PhraseDetectionService::class.java)
    }
}
