package com.pulselink.data.sms

import android.util.Log
import com.pulselink.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.pulselink.domain.model.PulseLinkSettings
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class SmsSyncManager @Inject constructor(
    private val smsRepository: SmsRepository,
    private val smsSyncTrigger: SmsSyncTrigger,
    private val settingsRepository: SettingsRepository
) {
    @androidx.annotation.VisibleForTesting
    var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isStarted = AtomicBoolean(false)

    @OptIn(FlowPreview::class)
    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        // 1. Observe SMS DB changes
        smsRepository.changes()
            .debounce(500L) // Debounce for 0.5 seconds to batch rapid changes
            .onEach {
                try {
                    val settings = settingsRepository.settings.first()
                    if (settings.remoteWebAccessEnabled) {
                        Log.d(TAG, "Repository changed, triggering web sync")
                        smsSyncTrigger.triggerSync()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking settings for sync", e)
                }
            }
            .launchIn(scope)

        // 2. Observe Settings changes to immediately sync when Premium/Web Access changes
        scope.launch {
            var previousSettings: PulseLinkSettings? = null
            settingsRepository.settings.collect { currentSettings ->
                val prev = previousSettings
                previousSettings = currentSettings

                if (prev == null) {
                    // Initial load: trigger sync if enabled to ensure consistency
                    if (currentSettings.remoteWebAccessEnabled) {
                        smsSyncTrigger.triggerSync()
                    }
                    return@collect
                }

                // Automatically enable Remote Web Access when upgrading to Premium
                if (!prev.premiumUnlocked && currentSettings.premiumUnlocked && !currentSettings.remoteWebAccessEnabled) {
                    Log.i(TAG, "Premium upgraded, enabling remote web access")
                    settingsRepository.setRemoteWebAccessEnabled(true)
                    // The update will trigger a new emission, so we don't need to sync here
                    return@collect
                }

                val shouldSync = (prev.remoteWebAccessEnabled != currentSettings.remoteWebAccessEnabled) ||
                    (prev.premiumUnlocked != currentSettings.premiumUnlocked) ||
                    (prev.proUnlocked != currentSettings.proUnlocked)

                if (shouldSync) {
                    Log.d(TAG, "Settings changed (premium/web), triggering sync worker")
                    smsSyncTrigger.triggerSync()
                }
            }
        }
    }

    companion object {
        private const val TAG = "SmsSyncManager"
    }
}
