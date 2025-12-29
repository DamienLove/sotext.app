package com.pulselink.service

import com.pulselink.data.alert.AlertDispatcher
import com.pulselink.data.alert.AlertDispatcher.AlertResult
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.repository.AlertRepository
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRouter @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val alertRepository: AlertRepository,
    private val dispatcher: AlertDispatcher
) {
    private val mutex = Mutex()

    suspend fun onPhraseDetected(phrase: String) {
        mutex.withLock {
            val settings = settingsRepository.settings.first()
            val normalized = phrase.lowercase().trim()
            val phrases = settings.phrases()
            val matchIndex = phrases.indexOfFirst { normalized.contains(it) }
            if (matchIndex == -1) return
            val tier = if (matchIndex == 0) EscalationTier.EMERGENCY else EscalationTier.CHECK_IN
            route(tier, normalized, settings)
        }
    }

    suspend fun dispatchManual(tier: EscalationTier, trigger: String) {
        mutex.withLock {
            val settings = settingsRepository.settings.first()
            route(tier, trigger, settings)
        }
    }

    suspend fun onInboundMessage(body: String) {
        val sanitized = body.lowercase()
        if (sanitized.contains("ack pulselink")) {
            alertRepository.record(
                AlertEvent(
                    timestamp = System.currentTimeMillis(),
                    triggeredBy = "Inbound acknowledgement",
                    tier = EscalationTier.CHECK_IN,
                    contactCount = 0,
                    sentSms = false,
                    sharedLocation = false
                )
            )
        }
    }

    private suspend fun route(
        tier: EscalationTier,
        trigger: String,
        settings: com.pulselink.domain.model.PulseLinkSettings
    ) {
        val contacts = when (tier) {
            EscalationTier.EMERGENCY -> contactRepository.getEmergencyContacts()
            EscalationTier.CHECK_IN -> contactRepository.getCheckInContacts()
        }
        if (contacts.isEmpty()) return
        val result: AlertResult = dispatcher.dispatch(trigger, tier, contacts, settings)
        alertRepository.record(
            AlertEvent(
                timestamp = System.currentTimeMillis(),
                triggeredBy = trigger,
                tier = tier,
                contactCount = contacts.size,
                sentSms = result.notifiedContacts > 0,
                sharedLocation = result.sharedLocation
            )
        )
    }
}
