package com.pulselink.ui

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pulselink.R
import com.pulselink.data.assistant.NaturalLanguageCommandProcessor
import com.pulselink.data.assistant.VoiceCommandResult
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.model.EscalationTier
import com.pulselink.service.AlertRouter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssistantShortcutActivity : FragmentActivity() {

    @Inject lateinit var alertRouter: AlertRouter
    @Inject lateinit var naturalLanguageCommandProcessor: NaturalLanguageCommandProcessor
    @Inject lateinit var contactLinkManager: ContactLinkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Debug logging for Assistant integration troubleshooting
        android.util.Log.d("AssistantShortcut", "onCreate called")
        android.util.Log.d("AssistantShortcut", "Intent action: ${intent?.action}")
        android.util.Log.d("AssistantShortcut", "Intent data: ${intent?.data}")
        android.util.Log.d("AssistantShortcut", "Intent extras: ${intent?.extras}")
        intent?.extras?.let { extras ->
            for (key in extras.keySet()) {
                android.util.Log.d("AssistantShortcut", "  Extra: $key = ${extras.get(key)}")
            }
        }        
        val intent = intent ?: run {
            finishSilently()
            return
        }

        // Sentinel: Removed insecure message/call handlers. Only explicit feature commands are allowed.
        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data ?: run {
                finishSilently()
                return
            }
            // Fall-through to feature extraction.
            // Undocumented paths like /assistant/message are no longer supported.
        }

        val feature = extractDeepLinkFeature(intent)
        if (!feature.isNullOrBlank()) {
            lifecycleScope.launch { handleShortcutFeature(feature) }
            return
        }
        val voiceQuery = extractVoiceQuery(intent)
        if (!voiceQuery.isNullOrBlank()) {
            lifecycleScope.launch { handleVoiceCommand(voiceQuery) }
            return
        }
        val action = intent.action
        if (action == null) {
            finishSilently()
            return
        }
        lifecycleScope.launch {
            when (action) {
                ACTION_ASSISTANT_EMERGENCY -> triggerAlert(EscalationTier.EMERGENCY)
                ACTION_ASSISTANT_CHECK_IN -> triggerAlert(EscalationTier.CHECK_IN)
                else -> finishSilently()
            }
        }
    }

    private suspend fun handleVoiceCommand(query: String) {
        val result = naturalLanguageCommandProcessor.handleCommand(query)
        val message = when (result) {
            is VoiceCommandResult.Success -> result.message
            is VoiceCommandResult.Error -> result.message
            VoiceCommandResult.UpgradeRequired -> getString(R.string.voice_command_upgrade_required)
        }
        toast(message)
        finish()
    }

    private suspend fun handleShortcutFeature(feature: String) {
        when (feature.lowercase(Locale.US)) {
            FEATURE_EMERGENCY -> triggerAlert(EscalationTier.EMERGENCY)
            FEATURE_CANCEL -> handleCancelEmergency()
            FEATURE_CHECK_IN -> triggerAlert(EscalationTier.CHECK_IN)
            else -> finishSilently()
        }
    }

    private fun extractVoiceQuery(intent: Intent?): String? {
        if (intent == null) return null
        return listOf(
            intent.getStringExtra(EXTRA_VOICE_QUERY),
            intent.getStringExtra(Intent.EXTRA_TEXT),
            intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT),
            intent.getStringExtra(SearchManager.QUERY)
        ).firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    /**
     * Extract feature parameter from both deep links (pulselink://assistant/{feature})
     * and App Links (https://pulselink.app/assistant/{feature})
     * 
     * @param intent The intent containing the link
     * @return The feature name (e.g., "emergency", "checkin", "cancel") or null if invalid
     */
    private fun extractDeepLinkFeature(intent: Intent?): String? {
        val data = intent?.data ?: return null
        
        android.util.Log.d("AssistantShortcut", "Extracting feature from URI: $data")
        
        if (intent.action != Intent.ACTION_VIEW) {
            android.util.Log.w("AssistantShortcut", "Invalid action: ${intent.action}, expected ACTION_VIEW")
            return null
        }
        
        // Handle deep links (pulselink://assistant/{feature})
        if (data.scheme == "pulselink") {
            if (data.host != "assistant") {
                android.util.Log.w("AssistantShortcut", "Invalid deep link host: ${data.host}")
                return null
            }
            val feature = data.lastPathSegment
            android.util.Log.d("AssistantShortcut", "Extracted feature from deep link: $feature")
            return feature
        }
        
        // Handle App Links (https://pulselink.app/assistant/{feature})
        if (data.scheme == "https") {
            // Validate that the host matches our verified domain
            if (data.host != APP_LINKS_HOST) {
                android.util.Log.w("AssistantShortcut", "Security: Rejecting unverified domain: ${data.host}")
                Toast.makeText(this, "Invalid link domain", Toast.LENGTH_SHORT).show()
                return null
            }
            
            // Check that path starts with /assistant/
            val path = data.path ?: ""
            if (!path.startsWith("/assistant/")) {
                android.util.Log.w("AssistantShortcut", "Invalid App Link path: $path")
                return null
            }
            
            // Extract feature from path (e.g., /assistant/emergency -> emergency)
            val feature = data.lastPathSegment
            android.util.Log.d("AssistantShortcut", "Extracted feature from App Link: $feature")
            return feature
        }
        
        android.util.Log.w("AssistantShortcut", "Unsupported scheme: ${data.scheme}")
        return null
    }

    private suspend fun triggerAlert(tier: EscalationTier) {
        val statusMessage = when (tier) {
            EscalationTier.EMERGENCY -> getString(R.string.assistant_trigger_emergency)
            EscalationTier.CHECK_IN -> getString(R.string.assistant_trigger_check_in)
        }
        alertRouter.dispatchManual(tier, statusMessage)
        toast(statusMessage)
        finish()
    }

    private suspend fun handleCancelEmergency() {
        val cancelled = contactLinkManager.cancelActiveEmergency()
        val message = if (cancelled) {
            getString(R.string.voice_command_emergency_cancelled)
        } else {
            getString(R.string.voice_command_cancel_failed)
        }
        toast(message)
        finish()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun finishSilently() {
        finish()
    }

    companion object {
        const val ACTION_ASSISTANT_EMERGENCY = "com.pulselink.intent.ASSISTANT_EMERGENCY"
        const val ACTION_ASSISTANT_CHECK_IN = "com.pulselink.intent.ASSISTANT_CHECK_IN"
        const val ACTION_ASSISTANT_VOICE = "com.pulselink.intent.ASSISTANT_VOICE"
        const val EXTRA_VOICE_QUERY = "com.pulselink.extra.VOICE_QUERY"
        private const val FEATURE_EMERGENCY = "emergency"
        private const val FEATURE_CANCEL = "cancel"
        private const val FEATURE_CHECK_IN = "checkin"
        
        // App Links host for validation
        private const val APP_LINKS_HOST = "pulselink.app"
    }
}
