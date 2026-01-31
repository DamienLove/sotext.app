package com.pulselink.beacon.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.pulselink.beacon.R
import com.pulselink.beacon.data.SmsRepository
import com.pulselink.beacon.data.SmsThreadItem
import com.pulselink.beacon.notifications.MessageNotificationManager
import kotlinx.coroutines.runBlocking

class BeaconWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return BeaconRemoteViewsFactory(this.applicationContext)
    }
}

class BeaconRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var threads: List<SmsThreadItem> = emptyList()
    private val repo = SmsRepository(context)

    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        // Fetch data synchronously
        runBlocking {
            threads = try {
                repo.listThreads(limit = 20)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override fun onDestroy() {
        // No-op
    }

    override fun getCount(): Int = threads.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in threads.indices) return RemoteViews(context.packageName, R.layout.beacon_widget_item)

        val thread = threads[position]
        val views = RemoteViews(context.packageName, R.layout.beacon_widget_item)

        views.setTextViewText(R.id.widget_item_title, thread.address)
        views.setTextViewText(R.id.widget_item_snippet, thread.snippet)

        val time = DateUtils.getRelativeTimeSpanString(
            thread.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        views.setTextViewText(R.id.widget_item_date, time)

        // Fill-in Intent
        val fillInIntent = Intent().apply {
            putExtra(MessageNotificationManager.EXTRA_THREAD_ID, thread.threadId)
            putExtra(MessageNotificationManager.EXTRA_ADDRESS, thread.address)
        }
        views.setOnClickFillInIntent(R.id.widget_item_title, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_snippet, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_item_date, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = threads.getOrNull(position)?.threadId ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
