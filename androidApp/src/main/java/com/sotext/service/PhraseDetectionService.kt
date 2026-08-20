package com.sotext.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sotext.R
import com.sotext.data.alert.NotificationRegistrar
import com.sotext.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps [SpeechRecognizer] listening in short bursts so a user's
 * configured safety phrase (see [com.sotext.domain.model.PulseLinkSettings.phrases]) can trigger
 * an alert hands-free, even while the app is backgrounded or the screen is off.
 *
 * Android's [SpeechRecognizer] only ever listens for a single utterance at a time, so
 * "continuous" listening here means: start a session, wait for a final result or an error, then
 * immediately start a new session. Recoverable errors (timeouts, no match, busy) restart quickly;
 * repeated failures back off to avoid a tight loop draining the battery, and losing the
 * RECORD_AUDIO permission or the settings toggle stops the service outright.
 */
@AndroidEntryPoint
class PhraseDetectionService : Service(), RecognitionListener {

    @Inject lateinit var alertRouter: AlertRouter
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationRegistrar: NotificationRegistrar

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var consecutiveErrors = 0
    private var lastDispatchAtMillis = 0L
    private var stopped = false

    // Starts true (prefer the on-device model when present). Permanently flips to false the
    // first time the recognizer reports the offline language pack is missing/unsupported, so
    // we fall back to network recognition instead of retrying the same failing request forever.
    private var preferOffline = true

    override fun onCreate() {
        super.onCreate()
        if (!hasRecordAudioPermission() || !SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Missing RECORD_AUDIO permission or no recognizer available; stopping")
            stopSelf()
            return
        }

        notificationRegistrar.ensureChannels()
        startForeground(NOTIFICATION_ID, buildNotification())

        mainHandler.post {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { speechRecognizer = it }
            recognizer.setRecognitionListener(this)
            startListeningSession()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        stopped = true
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            speechRecognizer?.let {
                runCatching { it.stopListening() }
                runCatching { it.destroy() }
            }
            speechRecognizer = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun startListeningSession() {
        if (stopped) return
        if (!hasRecordAudioPermission()) {
            Log.w(TAG, "RECORD_AUDIO permission revoked; stopping")
            stopSelf()
            return
        }
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            // Prefer an on-device model when the device has one downloaded. Some recognizer
            // implementations don't fall back to network on their own when the offline model
            // is missing (see onError's LANGUAGE_UNAVAILABLE/NOT_SUPPORTED handling below), so
            // we track that ourselves via preferOffline.
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { recognizer.startListening(intent) }
            .onFailure { error ->
                Log.w(TAG, "Failed to start listening session", error)
                scheduleRestart()
            }
    }

    private fun scheduleRestart(delayMillis: Long = RESTART_DELAY_MS) {
        if (stopped) return
        mainHandler.postDelayed({ startListeningSession() }, delayMillis)
    }

    private fun handlePhrase(text: String) {
        val now = System.currentTimeMillis()
        if (now - lastDispatchAtMillis < DISPATCH_COOLDOWN_MS) return
        serviceScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.passiveListeningEnabled) {
                Log.i(TAG, "Passive listening disabled mid-session; stopping")
                stopSelf()
                return@launch
            }
            val normalized = text.lowercase().trim()
            if (settings.phrases().any { normalized.contains(it) }) {
                lastDispatchAtMillis = now
                runCatching { alertRouter.onPhraseDetected(normalized) }
                    .onFailure { error -> Log.e(TAG, "Failed to route detected phrase", error) }
            }
        }
    }

    // --- RecognitionListener ---

    override fun onResults(results: Bundle?) {
        consecutiveErrors = 0
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.forEach { handlePhrase(it) }
        scheduleRestart(IMMEDIATE_RESTART_DELAY_MS)
    }

    override fun onError(error: Int) {
        when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                Log.w(TAG, "Speech recognizer reports missing permission; stopping")
                stopSelf()
                return
            }
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // Expected during silence; restart quickly without counting as a failure.
                consecutiveErrors = 0
                scheduleRestart(IMMEDIATE_RESTART_DELAY_MS)
                return
            }
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> {
                // The on-device model for this locale isn't downloaded (or doesn't exist).
                // Retrying the same offline-only request would just fail identically forever,
                // so permanently switch this session to network recognition and retry now.
                if (preferOffline) {
                    Log.w(TAG, "Offline language pack unavailable (error=$error); falling back to network recognition")
                    preferOffline = false
                }
                consecutiveErrors = 0
                scheduleRestart(IMMEDIATE_RESTART_DELAY_MS)
                return
            }
            else -> {
                consecutiveErrors = (consecutiveErrors + 1).coerceAtMost(MAX_BACKOFF_STEPS)
            }
        }
        val backoffMillis = RESTART_DELAY_MS * (1L shl (consecutiveErrors - 1).coerceAtLeast(0))
        scheduleRestart(backoffMillis.coerceAtMost(MAX_RESTART_DELAY_MS))
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun buildNotification() =
        NotificationCompat.Builder(this, NotificationRegistrar.CHANNEL_BACKGROUND)
            .setContentTitle(getString(R.string.passive_listening_notification_title))
            .setContentText(getString(R.string.passive_listening_notification_text))
            .setSmallIcon(R.drawable.ic_logo)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val TAG = "PhraseDetectionSvc"
        private const val NOTIFICATION_ID = 1003
        private const val IMMEDIATE_RESTART_DELAY_MS = 250L
        private const val RESTART_DELAY_MS = 1_500L
        private const val MAX_RESTART_DELAY_MS = 30_000L
        private const val MAX_BACKOFF_STEPS = 5
        private const val DISPATCH_COOLDOWN_MS = 10_000L

        fun newIntent(context: Context) = Intent(context, PhraseDetectionService::class.java)
    }
}
