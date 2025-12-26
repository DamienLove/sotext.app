package com.pulselink.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.RemoteViews
import com.pulselink.R
import com.pulselink.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@AndroidEntryPoint
class PulseLinkWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetProviderEntryPoint {
        fun widgetStateManager(): WidgetStateManager
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_pulselink)

        // Header
        val settingsIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val settingsPendingIntent = PendingIntent.getActivity(
            context, 0, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_settings_button, settingsPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_app_icon, settingsPendingIntent)

        // Compose Action
        val composeIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_SENDTO
            data = Uri.parse("sms:")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val composePendingIntent = PendingIntent.getActivity(
            context, 4, composeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_compose_button, composePendingIntent)

        // Actions
        val emergencyIntent = Intent(context, PulseLinkWidgetActionReceiver::class.java).apply {
            action = PulseLinkWidgetActionReceiver.ACTION_EMERGENCY
        }
        val emergencyPendingIntent = PendingIntent.getBroadcast(
            context, 1, emergencyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_emergency_action, emergencyPendingIntent)

        val checkinIntent = Intent(context, PulseLinkWidgetActionReceiver::class.java).apply {
            action = PulseLinkWidgetActionReceiver.ACTION_CHECKIN
        }
        val checkinPendingIntent = PendingIntent.getBroadcast(
            context, 2, checkinIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_checkin_action, checkinPendingIntent)

        // Shortcuts
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetProviderEntryPoint::class.java)
        val stateManager = entryPoint.widgetStateManager()
        val cachedContacts = WidgetCache.readContacts(context)
        val hasContactsCache = WidgetCache.hasContactsCache(context)
        val placeholderContacts = listOf(
            WidgetContact(id = -1L, displayName = "Loading"),
            WidgetContact(id = -2L, displayName = "Loading"),
            WidgetContact(id = -3L, displayName = "Loading")
        )
        val contacts = if (cachedContacts.isEmpty() && !hasContactsCache) {
            placeholderContacts
        } else {
            cachedContacts
        }

        views.removeAllViews(R.id.widget_shortcuts_row)
        contacts.take(4).forEach { contact ->
            val itemView = RemoteViews(context.packageName, R.layout.widget_contact_shortcut_item)
            val displayName = contact.displayName.ifBlank { "Contact" }
            itemView.setTextViewText(R.id.shortcut_name, displayName)
            itemView.setImageViewBitmap(R.id.shortcut_avatar, generateAvatar(displayName))

            // On Click -> Open Thread or Call? "Contact shortcuts"
            // Let's open the app with contact ID or just main app.
            // Opening MainActivity is safest.
            if (contact.id >= 0L) {
                val contactIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("contact_id", contact.id)
                }
                val contactPendingIntent = PendingIntent.getActivity(
                    context,
                    contact.id.toInt(),
                    contactIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                itemView.setOnClickPendingIntent(R.id.shortcut_avatar, contactPendingIntent)
            }

            views.addView(R.id.widget_shortcuts_row, itemView)
        }
        // Add spacers? RemoteViews linear layout just stacks them.
        // widget_contact_shortcut_item has padding/margin.

        // List
        val listIntent = Intent(context, PulseLinkWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, listIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)
        val cachedThreads = WidgetCache.readThreads(context)
        val hasThreadsCache = WidgetCache.hasThreadsCache(context)
        val emptyMessage = if (cachedThreads.isEmpty() && !hasThreadsCache) {
            "Loading messages..."
        } else {
            "No recent messages"
        }
        views.setTextViewText(R.id.widget_empty_view, emptyMessage)

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = Intent.ACTION_VIEW
        }
        val clickPendingTemplate = PendingIntent.getActivity(
            context, 3, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list, clickPendingTemplate)

        appWidgetManager.updateAppWidget(appWidgetId, views)

        if ((cachedContacts.isEmpty() && !hasContactsCache) || WidgetCache.shouldRefreshContacts(context)) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val fresh = runCatching { stateManager.getEmergencyContactsForWidget() }
                    .getOrDefault(emptyList())
                val payload = fresh.map { contact ->
                    WidgetContact(id = contact.id, displayName = contact.displayName)
                }
                WidgetCache.writeContacts(context, payload)
                stateManager.requestWidgetUpdate()
            }
        }
    }

    private fun generateAvatar(name: String): Bitmap {
        val size = 96 // larger for density
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val colors = listOf(
            0xFFEF5350.toInt(), 0xFFAB47BC.toInt(), 0xFF5C6BC0.toInt(),
            0xFF29B6F6.toInt(), 0xFF26A69A.toInt(), 0xFF66BB6A.toInt(),
            0xFFFFA726.toInt(), 0xFF8D6E63.toInt()
        )
        val colorIndex = (name.hashCode().absoluteValue) % colors.size
        paint.color = colors[colorIndex]
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = -1
        paint.textSize = 40f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER

        val initials = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifEmpty { "?" }

        val xPos = size / 2f
        val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2)

        canvas.drawText(initials, xPos, yPos, paint)
        return bitmap
    }
}
