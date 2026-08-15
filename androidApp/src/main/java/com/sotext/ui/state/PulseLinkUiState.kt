package com.sotext.ui.state

import androidx.annotation.StringRes
import com.sotext.domain.model.AlertEvent
import com.sotext.domain.model.Contact
import com.sotext.domain.model.PulseLinkSettings
import com.sotext.domain.model.SoundOption

data class PulseLinkUiState(
    val settings: PulseLinkSettings = PulseLinkSettings(),
    val contacts: List<Contact> = emptyList(),
    val recentEvents: List<AlertEvent> = emptyList(),
    val unreadAlertCount: Int = 0,
    val isDispatching: Boolean = false,
    val lastMessagePreview: String? = null,
    val emergencySoundOptions: List<SoundOption> = emptyList(),
    val checkInSoundOptions: List<SoundOption> = emptyList(),
    val callSoundOptions: List<SoundOption> = emptyList(),
    val showAds: Boolean = false,
    val isProUser: Boolean = true,
    val isPremiumUser: Boolean = false,
    val adsAvailable: Boolean = false,
    val onboardingComplete: Boolean = false,
    val dndStatus: DndStatusMessage? = null,
    val isEmergencyActive: Boolean = false,
    val autoUpdateContactInfo: Boolean = true,
    val profileUpdate: ProfileUpdateUiState = ProfileUpdateUiState()
)

data class DndStatusMessage(
    @StringRes val messageResId: Int
)

data class ProfileUpdateUiState(
    val inProgress: Boolean = false,
    val resultCount: Int? = null,
    val error: String? = null
)
