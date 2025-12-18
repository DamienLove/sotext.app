package com.pulselink.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.pulselink.R
import com.pulselink.data.sms.SmsRepository
import com.pulselink.data.sms.SmsThreadItem
import com.pulselink.domain.repository.ContactRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

class PulseLinkWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PulseLinkWidgetFactory(this.applicationContext)
    }
}

class PulseLinkWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetFactoryEntryPoint {
        fun smsRepository(): SmsRepository
        fun contactRepository(): ContactRepository
    }

    private lateinit var smsRepository: SmsRepository
    private lateinit var contactRepository: ContactRepository
    private var items: List<SmsThreadItem> = emptyList()

    override fun onCreate() {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetFactoryEntryPoint::class.java)
        smsRepository = entryPoint.smsRepository()
        contactRepository = entryPoint.contactRepository()
    }

    override fun onDataSetChanged() {
        val identityToken = android.os.Binder.clearCallingIdentity()
        try {
            items = smsRepository.listThreads(limit = 20)
        } catch (e: Exception) {
            e.printStackTrace()
            items = emptyList()
        } finally {
            android.os.Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= items.size) return RemoteViews(context.packageName, R.layout.widget_list_item)
        val item = items[position]

        val views = RemoteViews(context.packageName, R.layout.widget_list_item)

        val displayName = item.address.split(" · ").firstOrNull() ?: item.address

        // Use SpannableString for bolding unread messages
        val titleText = SpannableString(displayName)
        if (item.unread) {
             titleText.setSpan(StyleSpan(Typeface.BOLD), 0, titleText.length, 0)
             views.setTextColor(R.id.widget_item_title, context.getColor(android.R.color.white))
        } else {
             views.setTextColor(R.id.widget_item_title, context.getColor(R.color.widget_text_secondary))
        }
        views.setTextViewText(R.id.widget_item_title, titleText)
        views.setTextViewText(R.id.widget_item_snippet, item.snippet)

        val avatar = generateAvatar(displayName)
        views.setImageViewBitmap(R.id.widget_item_avatar, avatar)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        views.setTextViewText(R.id.widget_item_time, timeFormat.format(Date(item.timestamp)))

        val fillInIntent = Intent().apply {
            putExtra("thread_id", item.threadId)
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    private fun generateAvatar(name: String): Bitmap {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val colors = listOf(
            0xFFEF5350.toInt(),
            0xFFAB47BC.toInt(),
            0xFF5C6BC0.toInt(),
            0xFF29B6F6.toInt(),
            0xFF26A69A.toInt(),
            0xFF66BB6A.toInt(),
            0xFFFFA726.toInt(),
            0xFF8D6E63.toInt()
        )
        val colorIndex = (name.hashCode().absoluteValue) % colors.size
        paint.color = colors[colorIndex]
        paint.style = Paint.Style.FILL
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = -1 // White
        paint.textSize = 28f
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

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items.getOrNull(position)?.threadId ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
