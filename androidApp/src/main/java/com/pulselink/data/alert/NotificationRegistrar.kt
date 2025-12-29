package com.pulselink.data.alert

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.pulselink.R
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.model.SoundOption
import com.pulselink.util.resolveUri
import com.pulselink.util.VibrationPatterns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRegistrar @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannels() {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val group = NotificationChannelGroup(GROUP_ALERTS, context.getString(R.string.channel_alerts))
            manager.createNotificationChannelGroup(group)

            val background = NotificationChannel(
                CHANNEL_BACKGROUND,
                context.getString(R.string.channel_background),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setGroup(GROUP_ALERTS) }
            manager.createNotificationChannel(background)
        }
    }

    fun ensureSilentAlertChannel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return LEGACY_ALERT_CHANNEL
        }
        ensureChannels()
        val manager = context.getSystemService<NotificationManager>() ?: return LEGACY_ALERT_CHANNEL
        val existing = manager.getNotificationChannel(CHANNEL_ALERT_CONFIRMATION)
        if (existing != null) return CHANNEL_ALERT_CONFIRMATION
        val channel = NotificationChannel(
            CHANNEL_ALERT_CONFIRMATION,
            context.getString(R.string.notification_channel_alert_confirmations),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_alert_confirmations_desc)
            setGroup(GROUP_ALERTS)
            setBypassDnd(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
        return CHANNEL_ALERT_CONFIRMATION
    }

    fun ensureAlertChannel(
        category: SoundCategory,
        soundOption: SoundOption?,
        profile: AlertProfile
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return when (category) {
                SoundCategory.SIREN -> LEGACY_ALERT_CHANNEL
                SoundCategory.CHIME -> LEGACY_CHECK_IN_CHANNEL
                SoundCategory.CALL -> LEGACY_CALL_CHANNEL
            }
        }
        // Guarantee the notification group exists even if AlertDispatcher.ensureChannels()
        // has never been called on this install. Without the group, channel creation
        // throws and inbound manual messages never reach the conversation list.
        ensureChannels()

        val manager = context.getSystemService<NotificationManager>() ?: return LEGACY_ALERT_CHANNEL
        val channelId = buildChannelId(category, soundOption)
        Log.d(TAG, "Ensuring alert channel=$channelId category=$category sound=${soundOption?.key}")
        val existing = manager.getNotificationChannel(channelId)
        val soundUri = soundOption?.resolveUri(context)
        val desiredPattern = if (profile.vibrate) {
            VibrationPatterns.alertOption(profile.vibrationPatternKey).pattern
        } else {
            null
        }
        val needsUpdate = existing == null ||
            existing.shouldVibrate() != profile.vibrate ||
            (profile.vibrate && !patternsMatch(existing.vibrationPattern, desiredPattern)) ||
            existing.sound?.toString() != soundUri?.toString() ||
            existing.canBypassDnd() != profile.breakThroughDnd

        if (needsUpdate && existing != null) {
            manager.deleteNotificationChannel(channelId)
        }

        if (!needsUpdate && existing != null) {
            validateChannel(existing, profile)
            return channelId
        }
        val (name, importance, usage) = when (category) {
            SoundCategory.SIREN -> Triple(
                context.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_MAX,
                AudioAttributes.USAGE_ALARM
            )
            SoundCategory.CHIME -> Triple(
                context.getString(R.string.channel_check_ins),
                NotificationManager.IMPORTANCE_DEFAULT,
                AudioAttributes.USAGE_NOTIFICATION_EVENT
            )
            SoundCategory.CALL -> Triple(
                context.getString(R.string.channel_calls),
                NotificationManager.IMPORTANCE_HIGH,
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            )
        }

        val channelLabel = soundOption?.label?.let { "$name - $it" } ?: name
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(channelId, channelLabel, importance).apply {
            setGroup(GROUP_ALERTS)
            // On Android 15+ the interruption filter no longer disables global DND,
            // so channel-level bypass is the primary mechanism to punch through.
            setBypassDnd(profile.breakThroughDnd)
            enableVibration(profile.vibrate)
            if (profile.vibrate && desiredPattern != null) {
                vibrationPattern = desiredPattern
            }
            setSound(soundUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
        manager.getNotificationChannel(channelId)?.let {
            validateChannel(it, profile)
        } ?: Log.e(TAG, "Failed to create notification channel $channelId")
        return channelId
    }

    fun updateBadgeCount(count: Int) {
        // On Android 8.0+ (API 26+), badges are automatic based on active notifications.
        // We can't explicitly set the number on the badge for standard launchers.
        // The badge will show a dot or a number based on the number of active notifications
        // in the notification shade.
        // To properly support "unread count" badges, we rely on the summary notification
        // or individual notifications remaining active until read.
        // However, some 3rd party launchers support badge counts via intent, but that is
        // generally discouraged in modern Android development in favor of notification-based badges.

        // For this implementation, we rely on the notification management in AlertDispatcher
        // to ensure the notification count aligns with unread alerts.
    }

    private fun buildChannelId(category: SoundCategory, soundOption: SoundOption?): String {
        val base = when (category) {
            SoundCategory.SIREN -> "pulse_alert"
            SoundCategory.CHIME -> "pulse_checkin"
            SoundCategory.CALL -> "pulse_call"
        }
        val suffix = soundOption?.key ?: "default"
        return "${base}_$suffix"
    }

    private fun patternsMatch(existing: LongArray?, desired: LongArray?): Boolean {
        if (desired == null) return existing == null
        return existing?.contentEquals(desired) ?: false
    }

    private fun validateChannel(channel: NotificationChannel, profile: AlertProfile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (profile.breakThroughDnd && !channel.canBypassDnd()) {
            val hasPolicy =
                context.getSystemService<NotificationManager>()?.isNotificationPolicyAccessGranted == true
            Log.w(
                TAG,
                "Channel ${channel.id} canBypassDnd=false (hasPolicy=$hasPolicy). User may need to re-enable bypass."
            )
        }
        val expectedImportance = if (profile.breakThroughDnd) NotificationManager.IMPORTANCE_MAX
        else NotificationManager.IMPORTANCE_DEFAULT
        if (channel.importance < expectedImportance) {
            Log.w(TAG, "Channel ${channel.id} importance downgraded to ${channel.importance}")
        }
    }

        companion object {
            private const val TAG = "NotificationRegistrar"
            private const val LEGACY_ALERT_CHANNEL = "pulse_alerts_legacy"
            private const val LEGACY_CHECK_IN_CHANNEL = "pulse_checkins_legacy"
            private const val LEGACY_CALL_CHANNEL = "pulse_call_legacy"
            const val CHANNEL_BACKGROUND = "pulse_background"
            const val CHANNEL_ALERT_CONFIRMATION = "pulse_alert_confirmation"
            private const val GROUP_ALERTS = "pulse_group_alerts"
        }
}
