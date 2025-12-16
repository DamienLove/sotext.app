package com.pulselink.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.pulselink.data.sms.SmsSender

/**
 * Handles quick replies from call screen / notifications when the app is set as the
 * default SMS handler. Keeps the surface minimal but compliant.
 */
@AndroidEntryPoint
@android.annotation.SuppressLint("MissingPermission")
class RespondViaMessageService : Service() {

    @Inject lateinit var smsSender: SmsSender

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val dataUri = intent?.data
        val destination = dataUri?.schemeSpecificPart
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent?.getStringExtra("android.intent.extra.TEXT")
        if (destination.isNullOrBlank() || body.isNullOrBlank()) {
            Log.w(TAG, "RespondViaMessageService missing destination or body")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            runCatching {
                smsSender.sendSms(destination, body, awaitResult = false)
                Log.i(TAG, "Responded via message to $destination")
            }.onFailure { error ->
                Log.e(TAG, "Failed to send respond-via-message SMS", error)
            }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RespondViaMessageSvc"
    }
}
