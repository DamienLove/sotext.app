package com.sotext.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sotext.data.link.ContactLinkManager
import com.sotext.domain.model.EscalationTier
import com.sotext.service.AlertRouter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WearCommandListenerService : WearableListenerService() {

    @Inject lateinit var alertRouter: AlertRouter
    @Inject lateinit var contactLinkManager: ContactLinkManager

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != ACTION_PATH) return
        val payload = runCatching { String(event.data ?: ByteArray(0)) }.getOrNull() ?: return
        when (payload) {
            PAYLOAD_EMERGENCY -> dispatchEmergency()
            PAYLOAD_CANCEL -> cancelEmergency()
            PAYLOAD_CHECKIN -> dispatchCheckIn()
        }
    }

    private fun dispatchEmergency() {
        scope.launch { alertRouter.dispatchManual(EscalationTier.EMERGENCY, "Wear OS") }
    }

    private fun dispatchCheckIn() {
        scope.launch { alertRouter.dispatchManual(EscalationTier.CHECK_IN, "Wear OS") }
    }

    private fun cancelEmergency() {
        scope.launch { contactLinkManager.cancelActiveEmergency() }
    }

    companion object {
        const val ACTION_PATH = "/pulselink/action"
        const val PAYLOAD_EMERGENCY = "EMERGENCY"
        const val PAYLOAD_CANCEL = "CANCEL"
        const val PAYLOAD_CHECKIN = "CHECKIN"
    }
}
