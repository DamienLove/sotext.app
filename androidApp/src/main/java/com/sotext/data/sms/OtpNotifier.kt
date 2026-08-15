package com.sotext.data.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import android.content.ClipData
import android.content.ClipboardManager
import com.sotext.R

object OtpNotifier {
    private const val CHANNEL_ID = "pulse_otp_codes"
    private const val NOTIFICATION_ID = 7301

    fun notify(context: Context, sender: String, code: String) {
        copyToClipboard(context, code)
        showToast(context, code)
        ensureChannel(context)
        val title = context.getString(R.string.otp_notification_title)
        val body = context.getString(R.string.otp_notification_body, code)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nFrom: $sender"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun copyToClipboard(context: Context, code: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("2-step code", code))
    }

    private fun showToast(context: Context, code: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, context.getString(R.string.otp_toast_copied, code), Toast.LENGTH_LONG).show()
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.otp_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.otp_notification_channel_desc)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
