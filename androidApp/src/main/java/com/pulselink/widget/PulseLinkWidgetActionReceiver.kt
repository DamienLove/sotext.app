package com.pulselink.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pulselink.service.AlertRouter
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.ui.MainActivity
import com.pulselink.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.widget.Toast

@AndroidEntryPoint
class PulseLinkWidgetActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alertRouter: AlertRouter

    @Inject
    lateinit var contactLinkManager: ContactLinkManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var widgetStateManager: WidgetStateManager

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_EMERGENCY -> handleEmergency(context)
                    ACTION_CHECKIN -> handleCheckIn()
                    ACTION_NOTIFICATION -> handleNotification(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleEmergency(context: Context) {
        val isEmergencyActive = widgetStateManager.getEmergencyState()

        if (isEmergencyActive) {
            // If already active, clicking means we want to cancel (Biometric Auth)
            launchEmergencyPopup(context, "Active Alert", "EMERGENCY")
        } else {
            // Check for Double Tap
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val lastClickTime = prefs.getLong("last_click_time", 0L)
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime < 1000) {
                // Double Tap Detected -> Activate
                settingsRepository.setEmergencyActive(true)
                launchEmergencyPopup(context, "Emergency Activated", "EMERGENCY")
            } else {
                // First Tap -> show tooltip and save time
                Toast.makeText(
                    context,
                    context.getString(R.string.widget_instruction_short),
                    Toast.LENGTH_SHORT
                ).show()
                prefs.edit().putLong("last_click_time", currentTime).apply()
            }
        }
    }

    private fun launchEmergencyPopup(context: Context, contactName: String, tier: String) {
        val intent = Intent(context, com.pulselink.ui.EmergencyPopupActivity::class.java).apply {
            putExtra("extra_contact_name", contactName)
            putExtra("extra_escalation_tier", tier)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }

    private suspend fun handleCheckIn() {
        // Dispatch check-in alert
        alertRouter.dispatchManual(EscalationTier.CHECK_IN, "Widget check-in")
        widgetStateManager.requestWidgetUpdate()
    }

    private suspend fun handleNotification(context: Context) {
        widgetStateManager.markNotificationsSeen()
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(activityIntent)
    }

    companion object {
        const val ACTION_EMERGENCY = "com.pulselink.widget.EMERGENCY_ACTION"
        const val ACTION_CHECKIN = "com.pulselink.widget.CHECKIN_ACTION"
        const val ACTION_NOTIFICATION = "com.pulselink.widget.NOTIFICATION_ACTION"
    }
}
