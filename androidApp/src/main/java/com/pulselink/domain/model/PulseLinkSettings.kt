package com.pulselink.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertProfile(
    @SerialName("sound")
    val soundKey: String? = null,
    val breakThroughDnd: Boolean = true,
    val vibrate: Boolean = true
)

@Serializable
data class PulseLinkSettings(
    val primaryPhrase: String = "help me pulselink",
    val secondaryPhrase: String = "check in pulselink",
    val includeLocation: Boolean = true,
    val autoAllowRemoteSoundChange: Boolean = true,
    val assistantShortcutsDismissed: Boolean = false,
    val emergencyProfile: AlertProfile = AlertProfile(),
    val checkInProfile: AlertProfile = AlertProfile(
        breakThroughDnd = false,
        vibrate = true
    ),
    val callSoundKey: String? = null,
    val betaAgreementAccepted: Boolean = false,
    val betaAgreementVersion: String? = null,
    val autoCallAfterAlert: Boolean = false,
    val proUnlocked: Boolean = false,
    val premiumUnlocked: Boolean = false,
    val onboardingComplete: Boolean = false,
    val deviceId: String = "",
    val isBetaTester: Boolean = false,
    val ownerName: String = "",
    val autoUpdateContactInfo: Boolean = true,
    val timeFormat: TimeFormat = TimeFormat.AUTO,
    val themePreferences: ThemePreferences = ThemePreferences(),
    val remoteWebAccessEnabled: Boolean = false,
    val privatePinHash: String? = null,
    val privateThreadIds: List<Long> = emptyList(),
    val beaconLauncherEnabled: Boolean = true,
    val beaconHintDismissed: Boolean = false,
    val firebaseMessagingEnabled: Boolean = true,
    val emailFallbackEnabled: Boolean = false,
    val messagingChannelPriority: List<MessageChannel> = listOf(MessageChannel.FIREBASE, MessageChannel.SMS, MessageChannel.EMAIL)
) {
    fun phrases(): List<String> = listOf(primaryPhrase, secondaryPhrase)
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
}

enum class TimeFormat {
    AUTO, TWELVE_HOUR, TWENTY_FOUR_HOUR
}

@kotlinx.serialization.Serializable
data class ThemePreferences(
    val primaryColor: String = "#6750A4",
    val secondaryColor: String = "#625B71",
    val bubbleOutgoing: String = "#D0BCFF",
    val bubbleIncoming: String = "#E8DEF8",
    val backgroundColor: String = "#FFFFFF",
    val iconSizeFactor: Float = 1.0f
)
