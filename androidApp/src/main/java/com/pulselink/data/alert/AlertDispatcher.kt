package com.pulselink.data.alert

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import com.pulselink.R
import com.pulselink.data.location.LocationProvider
import com.pulselink.data.sms.SmsSender
import com.pulselink.data.emergency.EmergencyLocationRepository
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.model.SoundOption
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.util.AudioOverrideManager
import com.pulselink.util.resolveUri
import com.pulselink.util.VibrationPatterns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsSender: SmsSender,
    private val locationProvider: LocationProvider,
    private val registrar: NotificationRegistrar,
    private val soundCatalog: SoundCatalog,
    private val audioOverrideManager: AudioOverrideManager,
    private val emergencyLocationRepository: EmergencyLocationRepository
) {

    suspend fun dispatch(
        phrase: String,
        tier: EscalationTier,
        contacts: List<Contact>,
        settings: PulseLinkSettings,
        contactId: Long? = null,
        shouldPlayLocalSound: Boolean = false
    ): AlertResult = withContext(Dispatchers.IO) {
        registrar.ensureChannels()

        val locationInfo = if (settings.includeLocation) buildLocationInfo() else null
        val locationText = locationInfo?.message
        val message = buildMessage(phrase, tier, locationText)

        val (profile, soundCategory) = when (tier) {
            EscalationTier.EMERGENCY -> settings.emergencyProfile to SoundCategory.SIREN
            EscalationTier.CHECK_IN -> settings.checkInProfile to SoundCategory.CHIME
        }
        val soundOption = if (shouldPlayLocalSound) soundCatalog.resolve(profile.soundKey, soundCategory) else null
        val channelId = if (shouldPlayLocalSound) {
            registrar.ensureAlertChannel(soundCategory, soundOption, profile, settings.customVibrationPattern)
        } else {
            registrar.ensureSilentAlertChannel()
        }
        val orderedContacts = contacts.sortedWith(
            compareBy<Contact> { it.contactOrder }.thenBy { it.displayName.lowercase(Locale.US) }
        )

        val shouldOverrideAudio = shouldPlayLocalSound && (profile.breakThroughDnd || tier == EscalationTier.EMERGENCY)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val preFilter = notificationManager?.currentInterruptionFilter
        val overrideResult = if (shouldOverrideAudio) {
            Log.d(
                TAG,
                "Attempting audio override tier=$tier breakThrough=${profile.breakThroughDnd} sdk=${Build.VERSION.SDK_INT} hasPolicy=${notificationManager?.isNotificationPolicyAccessGranted}"
            )
            audioOverrideManager.overrideForAlert(profile.breakThroughDnd).also { result ->
                val postFilter = notificationManager?.currentInterruptionFilter
                if (!result.success) {
                    Log.w(
                        TAG,
                        "Audio override state=${result.state} reason=${result.reason} message=${result.message} preFilter=$preFilter postFilter=$postFilter"
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        Log.w(
                            TAG,
                            "Android 15+ only toggles the app's AutomaticZenRule; rely on channel bypass for full DND break-through."
                        )
                    }
                } else {
                    Log.d(TAG, "Audio override applied preFilter=$preFilter postFilter=$postFilter")
                }
            }
        } else {
            AudioOverrideManager.OverrideResult.skipped()
        }

        val smsCount = try {
            sendSms(tier, message, orderedContacts)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to send alert SMS", error)
            0
        }

        sendNotification(
            channel = channelId,
            tier = tier,
            message = message,
            profile = profile,
            soundOption = soundOption,
            primaryContact = orderedContacts.firstOrNull(),
            shouldPlaySound = shouldPlayLocalSound,
            notifiedContacts = smsCount,
            settings = settings
        )

        if (shouldOverrideAudio && overrideResult.state != AudioOverrideManager.OverrideResult.State.FAILURE && overrideResult.state != AudioOverrideManager.OverrideResult.State.SKIPPED) {
            audioOverrideManager.scheduleRestore()
        }

        if (tier == EscalationTier.EMERGENCY && locationInfo != null) {
            emergencyLocationRepository.recordOutgoing(
                location = locationInfo.location,
                message = message,
                sourceName = settings.ownerName.takeIf { it.isNotBlank() }
            )
        }

        val resolvedSoundKey = if (shouldPlayLocalSound) soundOption?.key ?: profile.soundKey else null
        AlertResult(
            message = message,
            notifiedContacts = smsCount,
            sharedLocation = locationText != null,
            overrideResult = overrideResult.takeIf { shouldOverrideAudio },
            soundKey = resolvedSoundKey,
            contactId = contactId
        )
    }

    private suspend fun buildLocationInfo(): LocationInfo? {
        val location = runCatching { locationProvider.lastKnownLocation() }.getOrNull() ?: return null
        val geoUri = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val timestamp = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())
        val message = "Last known location @ $timestamp: $geoUri"
        return LocationInfo(location = location, message = message)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun sendSms(
        tier: EscalationTier,
        message: String,
        contacts: List<Contact>
    ): Int {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "SEND_SMS permission not granted; skipping alert SMS dispatch.")
            return 0
        }
        val delay = if (tier == EscalationTier.EMERGENCY) SEQUENTIAL_CONTACT_DELAY_MS else 0L
        return smsSender.sendAlert(message, contacts, delay)
    }

    private fun buildMessage(phrase: String, tier: EscalationTier, locationText: String?): String {
        val header = when (tier) {
            EscalationTier.EMERGENCY -> "EMERGENCY" to "I need help right now."
            EscalationTier.CHECK_IN -> "I'M SAFE" to "I'm safe and sharing my location."
        }
        val builder = StringBuilder()
            .append("PulseLink ${header.first}: ${header.second}\n")
            .append("Phrase triggered: \"")
            .append(phrase)
            .append("\".")
        if (locationText != null) {
            builder.append("\n").append(locationText)
        }
        builder.append("\nThis message was sent automatically via PulseLink.")
        return builder.toString()
    }

    private fun sendNotification(
        channel: String,
        tier: EscalationTier,
        message: String,
        profile: AlertProfile,
        soundOption: SoundOption?,
        primaryContact: Contact?,
        shouldPlaySound: Boolean,
        notifiedContacts: Int,
        settings: PulseLinkSettings
    ) {
        val manager = NotificationManagerCompat.from(context)
        val (title, text) = if (shouldPlaySound) {
            val titleValue = if (tier == EscalationTier.EMERGENCY) {
                context.getString(R.string.notification_title_alert)
            } else {
                context.getString(R.string.notification_title_check_in)
            }
            titleValue to message
        } else {
            val titleValue = if (tier == EscalationTier.EMERGENCY) {
                context.getString(R.string.notification_title_alert_sent)
            } else {
                context.getString(R.string.notification_title_checkin_sent)
            }
            val countText = if (tier == EscalationTier.EMERGENCY) {
                context.getString(R.string.notification_text_alert_sent, notifiedContacts)
            } else {
                context.getString(R.string.notification_text_checkin_sent, notifiedContacts)
            }
            titleValue to countText
        }

        val notificationId = when (tier) {
            EscalationTier.EMERGENCY -> NOTIFICATION_ID_EMERGENCY
            EscalationTier.CHECK_IN -> NOTIFICATION_ID_CHECKIN
        }

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (shouldPlaySound) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_ALERTS)
            .setGroupSummary(false)

        if (shouldPlaySound && profile.vibrate) {
            val pattern = VibrationPatterns.patternForAlertKey(profile.vibrationPatternKey, settings.customVibrationPattern)
            builder.setVibrate(pattern)
        }
        if (shouldPlaySound && profile.breakThroughDnd) builder.setCategory(NotificationCompat.CATEGORY_ALARM)

        val soundUri = soundOption?.resolveUri(context)
        if (shouldPlaySound && soundUri != null) {
            builder.setSound(soundUri)
        }

        val callIntent = primaryContact?.let {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${'$'}{it.phoneNumber}"))
            PendingIntentCompat.getActivity(
                context,
                it.id.toInt(),
                dial,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false
            )
        }
        if (shouldPlaySound && callIntent != null) {
            builder.addAction(
                R.drawable.ic_logo,
                context.getString(R.string.notification_action_call),
                callIntent
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sysManager = context.getSystemService(NotificationManager::class.java)
            if (sysManager?.getNotificationChannel(channel) == null) {
                Log.w(TAG, "Expected notification channel $channel missing; re-registering")
                if (shouldPlaySound) {
                    registrar.ensureAlertChannel(
                        SoundCategory.SIREN.takeIf { tier == EscalationTier.EMERGENCY } ?: SoundCategory.CHIME,
                        soundOption,
                        profile,
                        settings.customVibrationPattern
                    )
                } else {
                    registrar.ensureSilentAlertChannel()
                }
            }
        }

        manager.notify(notificationId, builder.build())

        // Post a summary notification to group them if needed (mostly useful if we had multiple distinct notifications)
        // Since we are using constant IDs per tier, we might not strictly need this if we only show one per tier,
        // but it's good practice for grouping.
        val summaryNotification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_logo)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("PulseLink Alerts"))
            .setGroup(GROUP_KEY_ALERTS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_SUMMARY, summaryNotification)
    }

    data class AlertResult(
        val message: String,
        val notifiedContacts: Int,
        val sharedLocation: Boolean,
        val overrideResult: AudioOverrideManager.OverrideResult? = null,
        val soundKey: String? = null,
        val contactId: Long? = null
    )

    private data class LocationInfo(
        val location: android.location.Location,
        val message: String
    )

    companion object {
        private const val TAG = "AlertDispatcher"
        private const val SEQUENTIAL_CONTACT_DELAY_MS = 1_500L
        private const val GROUP_KEY_ALERTS = "PULSELINK_ALERTS"
        private const val NOTIFICATION_ID_EMERGENCY = 1001
        private const val NOTIFICATION_ID_CHECKIN = 1002
        private const val NOTIFICATION_ID_SUMMARY = 1000
    }
}
