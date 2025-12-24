package com.pulselink.data.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.ui.BeaconInboxActivity
import com.pulselink.util.normalizeSmsAddress

object MessageNotificationManager {
    const val CHANNEL_MESSAGES = "beacon_messages"
    private val DEFAULT_VIBRATION = longArrayOf(0, 250, 200, 250)

    const val EXTRA_THREAD_ID = "com.pulselink.extra.THREAD_ID"
    const val EXTRA_ADDRESS = "com.pulselink.extra.ADDRESS"

    fun notifyIncoming(
        context: Context,
        address: String,
        body: String,
        timestamp: Long,
        settings: PulseLinkSettings,
        threadId: Long? = null
    ) {
        if (!areNotificationsEnabled(context)) return

        val normalized = normalizeSmsAddress(address)
        val overrideSound = settings.messageNotificationSoundOverrides[normalized]
        val soundUri = overrideSound ?: settings.messageNotificationSoundUri
        val channelId = if (overrideSound != null) channelIdForContact(normalized) else CHANNEL_MESSAGES
        val title = resolveContactLabel(context, address).ifBlank { "New message" }

        ensureChannel(
            context = context,
            channelId = channelId,
            channelName = if (overrideSound != null) "Messages from $title" else "Beacon messages",
            soundUri = soundUri,
            vibrate = settings.messageNotificationVibrate
        )

        val contentIntent = buildContentIntent(context, threadId, address)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(resolveSoundUri(soundUri))
            if (settings.messageNotificationVibrate) {
                builder.setVibrate(DEFAULT_VIBRATION)
            }
        }

        val baseId = (threadId ?: normalized.hashCode().toLong()).hashCode()
        val notificationId = (baseId and 0xFFFF) + 8000
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isMessageChannelSilent(context: Context, channelId: String = CHANNEL_MESSAGES): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(channelId) ?: return false
        if (channel.importance == NotificationManager.IMPORTANCE_NONE) return true
        val hasSound = channel.sound != null
        val vibrates = channel.shouldVibrate()
        return !hasSound && !vibrates
    }

    fun buildNotificationSettingsIntent(context: Context, channelId: String = CHANNEL_MESSAGES): Intent {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun ensureChannel(
        context: Context,
        channelId: String,
        channelName: String,
        soundUri: String?,
        vibrate: Boolean
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val desiredSound = resolveSoundUri(soundUri)
        val existing = manager.getNotificationChannel(channelId)
        val needsUpdate = existing == null ||
            (existing.sound?.toString() != desiredSound.toString()) ||
            existing.shouldVibrate() != vibrate

        if (needsUpdate && existing != null) {
            manager.deleteNotificationChannel(channelId)
        }

        if (needsUpdate) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Beacon message notifications"
                enableVibration(vibrate)
                if (vibrate) {
                    vibrationPattern = DEFAULT_VIBRATION
                }
                setSound(
                    desiredSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildContentIntent(context: Context, threadId: Long?, address: String): PendingIntent {
        val intent = Intent(context, BeaconInboxActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (threadId != null && threadId > 0) {
                putExtra(EXTRA_THREAD_ID, threadId)
            }
            putExtra(EXTRA_ADDRESS, address)
        }
        val requestCode = (threadId ?: address.hashCode().toLong()).hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun resolveSoundUri(soundUri: String?): Uri {
        return soundUri?.let { Uri.parse(it) } ?: Settings.System.DEFAULT_NOTIFICATION_URI
    }

    private fun channelIdForContact(key: String): String =
        "${CHANNEL_MESSAGES}.${Integer.toHexString(key.hashCode())}"

    private fun resolveContactLabel(context: Context, address: String): String {
        val normalized = normalizeSmsAddress(address)
        if (normalized.isBlank()) return address
        return runCatching {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            val lookup = Uri.withAppendedPath(uri, Uri.encode(normalized))
            context.contentResolver.query(
                lookup,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                        ?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
        }.getOrNull() ?: address
    }
}
