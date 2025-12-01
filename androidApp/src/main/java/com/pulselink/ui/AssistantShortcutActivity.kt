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
import com.pulselink.data.link.RemoteAlertStatus
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.MessageUrgency
import com.pulselink.domain.model.VolumeHint
import com.pulselink.domain.repository.ContactRepository
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
    @Inject lateinit var contactRepository: ContactRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = intent ?: run {
            finishSilently()
            return
        }

        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data ?: run {
                finishSilently()
                return
            }
            when (data.path) {
                "/assistant/message" -> handleCreateMessageIntent(data).also { return }
                "/assistant/urgent" -> handleUrgentMessageIntent(data).also { return }
                "/assistant/nonurgent" -> handleNonUrgentMessageIntent(data).also { return }
            }
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

    private fun handleCreateMessageIntent(data: Uri) {
        val contactName = data.getQueryParameter("contact")
        val messageText = data.getQueryParameter("text")
        val volumeHint = parseVolumeHint(data.getQueryParameter("volume"))

        if (contactName.isNullOrBlank() || messageText.isNullOrBlank()) {
            Toast.makeText(this, "Contact or message text is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        lifecycleScope.launch {
            val targetContact = findContact(contactName)
            if (targetContact == null) {
                toast(getString(R.string.assistant_contact_not_found, contactName))
            } else {
                val result = contactLinkManager.sendManualMessage(
                    targetContact.id,
                    messageText,
                    MessageUrgency.STANDARD,
                    volumeHint
                )
                val toastMessage = if (result is ManualMessageResult.Success) {
                    getString(R.string.assistant_message_sent, targetContact.displayName)
                } else {
                    getString(R.string.assistant_message_failed, targetContact.displayName)
                }
                toast(toastMessage)
            }
            finish()
        }
    }

    private fun handleUrgentMessageIntent(data: Uri) {
        val contactName = data.getQueryParameter("contact")
        val messageText = data.getQueryParameter("text")
        val volumeHint = parseVolumeHint(data.getQueryParameter("volume")) ?: VolumeHint.HIGH

        if (contactName.isNullOrBlank()) {
            toast("Contact name is missing for urgent message.")
            finish()
            return
        }

        lifecycleScope.launch {
            val targetContact = findContact(contactName)
            if (targetContact == null) {
                toast(getString(R.string.assistant_contact_not_found, contactName))
            } else {
                val alertResult = contactLinkManager.triggerRemoteAlert(targetContact, EscalationTier.EMERGENCY)
                if (!messageText.isNullOrBlank()) {
                    contactLinkManager.sendManualMessage(
                        targetContact.id,
                        messageText,
                        MessageUrgency.URGENT,
                        volumeHint
                    )
                }
                val toastMessage = if (alertResult.status == RemoteAlertStatus.SUCCESS) {
                    getString(R.string.assistant_urgent_message_sent, targetContact.displayName)
                } else {
                    getString(R.string.assistant_urgent_message_failed, targetContact.displayName)
                }
                toast(toastMessage)
            }
            finish()
        }
    }
    
    private fun handleNonUrgentMessageIntent(data: Uri) {
        val contactName = data.getQueryParameter("contact")
        val messageText = data.getQueryParameter("text")
        val volumeQuery = data.getQueryParameter("volume")

        if (contactName.isNullOrBlank() || messageText.isNullOrBlank()) {
            toast("Contact or message text is missing.")
            finish()
            return
        }

        lifecycleScope.launch {
            val targetContact = findContact(contactName)
            if (targetContact == null) {
                toast(getString(R.string.assistant_contact_not_found, contactName))
            } else {
                val volumeHint = parseVolumeHint(volumeQuery) ?: VolumeHint.MEDIUM
                val result = contactLinkManager.sendManualMessage(
                    targetContact.id,
                    messageText,
                    MessageUrgency.STANDARD,
                    volumeHint
                )
                val toastMessage = if (result is ManualMessageResult.Success) {
                    getString(R.string.assistant_non_urgent_sent, targetContact.displayName, volumeHint.name.lowercase())
                } else {
                    getString(R.string.assistant_message_failed, targetContact.displayName)
                }
                toast(toastMessage)
            }
            finish()
        }
    }

    private suspend fun findContact(contactName: String) = 
        contactRepository.observeContacts().first()
            .firstOrNull { it.displayName.equals(contactName, ignoreCase = true) }


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

    private fun parseVolumeHint(raw: String?): VolumeHint? {
        val lower = raw?.lowercase(Locale.US)?.trim().orEmpty()
        return when {
            lower.isBlank() -> null
            lower.contains("low") || lower.contains("quiet") || lower.contains("half") -> VolumeHint.LOW
            lower.contains("high") || lower.contains("loud") || lower.contains("max") || lower.contains("full") -> VolumeHint.HIGH
            else -> VolumeHint.MEDIUM
        }
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

    private fun extractDeepLinkFeature(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (intent.action != Intent.ACTION_VIEW) return null
        if (data.scheme != "pulselink" || data.host != "assistant") return null
        return data.lastPathSegment
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
    }
}
