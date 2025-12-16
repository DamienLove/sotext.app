package com.pulselink.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.pulselink.R
import com.pulselink.ui.MainActivity
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.RemotePresence
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

class PulseLinkWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetProviderEntryPoint {
        fun widgetStateManager(): WidgetStateManager
    }

    private fun entryPoint(context: Context): WidgetProviderEntryPoint =
        EntryPointAccessors.fromApplication(context, WidgetProviderEntryPoint::class.java)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        runCatching { updateWidgetsSync(context, appWidgetManager, appWidgetIds) }
            .onFailure { provideFallback(context, appWidgetManager, appWidgetIds) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PulseLinkWidgetProvider::class.java))
            runCatching { updateWidgetsSync(context, mgr, ids) }
                .onFailure { provideFallback(context, mgr, ids) }
        }
    }

    @SuppressLint("RemoteViewLayout")
    private fun updateWidgetsSync(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val widgetStateManager = runCatching { entryPoint(context).widgetStateManager() }.getOrNull()
        if (widgetStateManager == null) {
            provideFallback(context, appWidgetManager, appWidgetIds)
            return
        }

        val isEmergencyActive = runBlocking {
            runCatching { widgetStateManager.getEmergencyState() }.getOrDefault(false)
        }
        val timeText = java.text.SimpleDateFormat("h:mm a").format(java.util.Date())

        val contacts = runBlocking {
            runCatching { widgetStateManager.getEmergencyContactsForWidget() }
                .getOrDefault(emptyList())
        }

        val calendar = java.util.Calendar.getInstance()
        val hourAngle = calendar.get(java.util.Calendar.HOUR) * 30f + calendar.get(java.util.Calendar.MINUTE) * 0.5f
        val minuteAngle = calendar.get(java.util.Calendar.MINUTE) * 6f

        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_pulselink)

            val emergencyIntent = Intent(context, PulseLinkWidgetActionReceiver::class.java).apply {
                action = PulseLinkWidgetActionReceiver.ACTION_EMERGENCY
            }
            val emergencyPendingIntent = PendingIntent.getBroadcast(
                context, 0, emergencyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_emergency_button, emergencyPendingIntent)
            // No text overlay on logo; tooltip handled in receiver

            val settingsIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val settingsPendingIntent = PendingIntent.getActivity(
                context, 3, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_settings_button, settingsPendingIntent)

            // Tooltip visibility and glow
            views.setViewVisibility(R.id.widget_tooltip, if (isEmergencyActive) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_center_glow, View.VISIBLE)

            // Clock hands rotation
            views.setFloat(R.id.widget_hand_hour, "setRotation", hourAngle)
            views.setFloat(R.id.widget_hand_minute, "setRotation", minuteAngle)
            views.setFloat(R.id.widget_hand_hour, "setPivotX", 2f)
            views.setFloat(R.id.widget_hand_hour, "setPivotY", 70f)
            views.setFloat(R.id.widget_hand_minute, "setPivotX", 1.5f)
            views.setFloat(R.id.widget_hand_minute, "setPivotY", 90f)

            runCatching { bindContacts(views, contacts, context) }
            runCatching { appWidgetManager.updateAppWidget(id, views) }
        }
    }

    private fun provideFallback(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val fallback = RemoteViews(context.packageName, R.layout.widget_pulselink)
        appWidgetIds.forEach { id -> runCatching { appWidgetManager.updateAppWidget(id, fallback) } }
    }

    private fun bindContacts(views: RemoteViews, contacts: List<Contact>, context: Context) {
        val rows = listOf(
            ContactRow(R.id.widget_row_1, R.id.contact_status_1, R.id.contact_name_1, R.id.contact_role_1),
            ContactRow(R.id.widget_row_2, R.id.contact_status_2, R.id.contact_name_2, R.id.contact_role_2),
            ContactRow(R.id.widget_row_3, R.id.contact_status_3, R.id.contact_name_3, R.id.contact_role_3),
            ContactRow(R.id.widget_row_4, R.id.contact_status_4, R.id.contact_name_4, R.id.contact_role_4),
            ContactRow(R.id.widget_row_5, R.id.contact_status_5, R.id.contact_name_5, R.id.contact_role_5)
        )

        val fallback = listOf(
            Placeholder("Dr. Emily", "PRIMARY PHYSICIAN", RemotePresence.ONLINE),
            Placeholder("Mom", "EMERGENCY CONTACT", RemotePresence.ONLINE),
            Placeholder("John (Husband)", "SPOUSE", RemotePresence.UNKNOWN),
            Placeholder("Sarah (Sister)", "SISTER", RemotePresence.ONLINE),
            Placeholder("Dad", "EMERGENCY CONTACT", RemotePresence.OFFLINE)
        )

        rows.forEachIndexed { index, row ->
            val contact = contacts.getOrNull(index)
            if (contact == null) {
                val placeholder = fallback.getOrNull(index)
                if (placeholder == null) {
                    views.setViewVisibility(row.containerId, View.GONE)
                } else {
                    views.setViewVisibility(row.containerId, View.VISIBLE)
                    views.setTextViewText(row.nameId, placeholder.name)
                    views.setTextViewText(row.roleId, placeholder.role)
                    views.setImageViewResource(row.statusId, placeholder.dotRes())
                }
            } else {
                views.setViewVisibility(row.containerId, View.VISIBLE)
                views.setTextViewText(row.nameId, contact.displayName)
                val role = when (contact.escalationTier) {
                    EscalationTier.EMERGENCY -> context.getString(R.string.widget_role_emergency)
                    EscalationTier.CHECK_IN -> context.getString(R.string.widget_role_checkin)
                }
                views.setTextViewText(row.roleId, role.uppercase())
                val dotRes = when (contact.remotePresence) {
                    RemotePresence.ONLINE, RemotePresence.RECENT -> R.drawable.widget_status_dot_green
                    RemotePresence.OFFLINE, RemotePresence.STALE -> R.drawable.widget_status_dot_red
                    RemotePresence.UNKNOWN -> R.drawable.widget_status_dot_gray
                }
                views.setImageViewResource(row.statusId, dotRes)
            }
        }
    }

    private data class ContactRow(
        val containerId: Int,
        val statusId: Int,
        val nameId: Int,
        val roleId: Int
    )

    private data class Placeholder(
        val name: String,
        val role: String,
        val presence: RemotePresence
    ) {
        fun dotRes(): Int = when (presence) {
            RemotePresence.ONLINE, RemotePresence.RECENT -> R.drawable.widget_status_dot_green
            RemotePresence.OFFLINE, RemotePresence.STALE -> R.drawable.widget_status_dot_red
            RemotePresence.UNKNOWN -> R.drawable.widget_status_dot_gray
        }
    }
}
