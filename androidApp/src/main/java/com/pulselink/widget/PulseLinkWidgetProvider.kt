package com.pulselink.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.pulselink.R
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
        updateWidgetsSync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, PulseLinkWidgetProvider::class.java))
            updateWidgetsSync(context, mgr, ids)
        }
    }

    private fun updateWidgetsSync(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val widgetStateManager = entryPoint(context).widgetStateManager()

        val (isEmergencyActive, unreadCount) = runBlocking {
            val active = runCatching { widgetStateManager.getEmergencyState() }.getOrDefault(false)
            val unread = runCatching { widgetStateManager.getUnreadEmergencyMessageCount() }.getOrDefault(0)
            active to unread
        }
        val timeText = java.text.SimpleDateFormat("h:mm a").format(java.util.Date())

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

            val checkInIntent = Intent(context, PulseLinkWidgetActionReceiver::class.java).apply {
                action = PulseLinkWidgetActionReceiver.ACTION_CHECKIN
            }
            val checkInPendingIntent = PendingIntent.getBroadcast(
                context, 1, checkInIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_checkin_button, checkInPendingIntent)

            if (unreadCount > 0) {
                views.setViewVisibility(R.id.widget_notification_container, View.VISIBLE)
                val badgeText = if (unreadCount > 9) "9+" else unreadCount.toString()
                views.setTextViewText(R.id.widget_notification_badge, badgeText)

                val notifIntent = Intent(context, PulseLinkWidgetActionReceiver::class.java).apply {
                    action = PulseLinkWidgetActionReceiver.ACTION_NOTIFICATION
                }
                val notifPendingIntent = PendingIntent.getBroadcast(
                    context, 2, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_notification_container, notifPendingIntent)
            } else {
                views.setViewVisibility(R.id.widget_notification_container, View.GONE)
            }

            views.setTextViewText(R.id.widget_clock_text, timeText)

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
