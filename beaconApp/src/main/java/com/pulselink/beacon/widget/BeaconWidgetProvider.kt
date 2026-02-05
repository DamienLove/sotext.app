package com.pulselink.beacon.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.pulselink.beacon.MainActivity
import com.pulselink.beacon.R
import com.pulselink.beacon.notifications.MessageNotificationManager

class BeaconWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.beacon_widget)

            // Intent to launch Main Activity (Inbox)
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_icon, pendingIntent)

            // Intent to launch New Message
            // We use the same MainActivity intent but with a special extra or just let MainActivity open default
            // Actually MainActivity opens Inbox by default.
            // If we want to open "newMessage", we can pass an extra.
            // But MainActivity's logic to open "newMessage" isn't exposed via simple extra except "Shared Content".
            // So we can just launch main activity for now.
            views.setOnClickPendingIntent(R.id.widget_compose, pendingIntent)

            // Set up collection adapter
            val serviceIntent = Intent(context, BeaconWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Set up item click template
            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context, 1, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
