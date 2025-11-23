package com.pulselink.widget

import android.content.Context
import android.content.Intent
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository
) {

    suspend fun getEmergencyState(): Boolean = settingsRepository.isEmergencyActive()

    suspend fun getUnreadEmergencyMessageCount(): Int {
        val lastCheck = settingsRepository.getLastWidgetCheckTimestamp()
        val emergencyContacts = contactRepository.getEmergencyContacts()
        val contactIds = emergencyContacts.map { it.id }
        if (contactIds.isEmpty()) return 0
        return messageRepository.getUnreadEmergencyMessageCount(contactIds, lastCheck)
    }

    suspend fun getOnlineFriends(): List<com.pulselink.domain.model.Contact> {
        return contactRepository.getAll()
            .filter { it.remotePresence == com.pulselink.domain.model.RemotePresence.ONLINE }
            .sortedBy { it.escalationTier } // Prioritize Emergency contacts
            .take(3)
    }

    suspend fun markNotificationsSeen() {
        settingsRepository.updateLastWidgetCheckTimestamp(System.currentTimeMillis())
        requestWidgetUpdate()
    }

    fun requestWidgetUpdate() {
        val intent = Intent(context, PulseLinkWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        // Use the provider to get IDs and broadcast
        val ids = android.appwidget.AppWidgetManager.getInstance(context)
            .getAppWidgetIds(android.content.ComponentName(context, PulseLinkWidgetProvider::class.java))
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }
}
