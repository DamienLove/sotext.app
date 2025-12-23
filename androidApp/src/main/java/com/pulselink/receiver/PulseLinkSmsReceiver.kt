package com.pulselink.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.data.link.RemoteActionHandler
import com.pulselink.data.sms.OtpHelper
import com.pulselink.data.sms.OtpNotifier
import com.pulselink.data.sms.SmsCodec
import com.pulselink.data.sms.SmsStore
import com.pulselink.service.AlertRouter
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.MessageUrgency
import com.pulselink.domain.model.VolumeHint
import android.telephony.PhoneNumberUtils

@AndroidEntryPoint
class PulseLinkSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var alertRouter: AlertRouter
    @Inject lateinit var contactLinkManager: ContactLinkManager
    @Inject lateinit var smsStore: SmsStore
    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var remoteActionHandler: RemoteActionHandler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val mergedBody = messages.joinToString(separator = "") { it.messageBody }
        val body = mergedBody.trim()
        if (body.isBlank()) return
        val origin = messages.firstOrNull()?.originatingAddress.orEmpty()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                try {
                    smsStore.insertIncoming(origin, body, System.currentTimeMillis())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert incoming SMS from $origin", e)
                }
                val completed = withTimeoutOrNull(8_000L) {
                    val parsed = SmsCodec.parse(body)
                    if (parsed != null) {
                        contactLinkManager.handleInbound(parsed, origin)
                    } else {
                        alertRouter.onInboundMessage(body)
                        val otpCode = if (OtpHelper.isOtpMessage(origin, body)) {
                            OtpHelper.extractCode(body)
                        } else {
                            null
                        }
                        if (otpCode != null) {
                            OtpNotifier.notify(context, origin, otpCode)
                        }
                        handleTrustedSms(origin, body)
                    }
                }
                if (completed == null) {
                    Log.w(TAG, "SMS processing timed out for sender: $origin")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to handle inbound SMS from $origin", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PulseLinkSmsReceiver"
    }

    private suspend fun handleTrustedSms(origin: String, body: String) {
        val normalized = PhoneNumberUtils.normalizeNumber(origin)
        val contact = contactRepository.getByPhone(origin)
            ?: contactRepository.getByPhone(normalized)
        if (contact == null) return

        val urgency = when {
            OtpHelper.isUrgentBody(body) -> MessageUrgency.URGENT
            contact.escalationTier == EscalationTier.EMERGENCY -> MessageUrgency.EMERGENCY
            else -> MessageUrgency.STANDARD
        }
        val tier = if (urgency == MessageUrgency.STANDARD) EscalationTier.CHECK_IN else EscalationTier.EMERGENCY
        val volumeHint = when (urgency) {
            MessageUrgency.EMERGENCY -> VolumeHint.MAX
            MessageUrgency.URGENT -> VolumeHint.HIGH
            MessageUrgency.STANDARD -> VolumeHint.MEDIUM
        }
        val title = "Message from ${contact.displayName}"
        val preview = body.take(160).ifBlank { "New message received." }
        remoteActionHandler.playAttentionTone(
            contact = contact,
            tier = tier,
            title = title,
            body = preview,
            notificationId = (contact.id.hashCode() and 0xFFFF) + 7000,
            forceBypass = true,
            volumeHint = volumeHint
        )
    }
}
