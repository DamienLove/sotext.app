package com.pulselink.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

private const val DATA_STORE_NAME = "pulselink_settings"

private val PRIMARY_PHRASE = stringPreferencesKey("primary_phrase")
private val SECONDARY_PHRASE = stringPreferencesKey("secondary_phrase")
private val INCLUDE_LOCATION = booleanPreferencesKey("include_location")
private val AUTO_ALLOW_REMOTE_SOUND = booleanPreferencesKey("auto_allow_remote_sound")
private val ASSISTANT_HINT_DISMISSED = booleanPreferencesKey("assistant_hint_dismissed")
private val EMERGENCY_PROFILE = stringPreferencesKey("emergency_profile")
private val CHECKIN_PROFILE = stringPreferencesKey("checkin_profile")
private val CALL_SOUND = stringPreferencesKey("call_sound")
private val BETA_AGREEMENT_ACCEPTED = booleanPreferencesKey("beta_agreement_accepted")
private val BETA_AGREEMENT_VERSION = stringPreferencesKey("beta_agreement_version")
private val AUTO_CALL = booleanPreferencesKey("auto_call")
private val PRO_UNLOCKED = booleanPreferencesKey("pro_unlocked")
private val PREMIUM_UNLOCKED = booleanPreferencesKey("premium_unlocked")
private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
private val DEVICE_ID = stringPreferencesKey("device_id")
private val OWNER_NAME = stringPreferencesKey("owner_name")
private val IS_BETA_TESTER = booleanPreferencesKey("is_beta_tester")
private val WIDGET_LAST_CHECK_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("widget_last_check_timestamp")
private val EMERGENCY_ACTIVE = booleanPreferencesKey("emergency_active")
private val LAST_KNOWN_PHONE = stringPreferencesKey("last_known_phone")
private val LAST_KNOWN_EMAIL = stringPreferencesKey("last_known_email")
private val AUTO_UPDATE_CONTACT_INFO = booleanPreferencesKey("auto_update_contact_info")
private val TIME_FORMAT = stringPreferencesKey("time_format")
private val THEME_PREFERENCES = stringPreferencesKey("theme_preferences")
private val REMOTE_WEB_ACCESS = booleanPreferencesKey("remote_web_access")
private val PRIVATE_PIN_HASH = stringPreferencesKey("private_pin_hash")
private val PRIVATE_THREADS = stringPreferencesKey("private_threads")
private val BEACON_LAUNCHER_ENABLED = booleanPreferencesKey("beacon_launcher_enabled")

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settings: Flow<PulseLinkSettings> = dataStore.data.map { prefs ->
        PulseLinkSettings(
            primaryPhrase = prefs[PRIMARY_PHRASE] ?: PulseLinkSettings().primaryPhrase,
            secondaryPhrase = prefs[SECONDARY_PHRASE] ?: PulseLinkSettings().secondaryPhrase,
            includeLocation = prefs[INCLUDE_LOCATION] ?: PulseLinkSettings().includeLocation,
            autoAllowRemoteSoundChange = prefs[AUTO_ALLOW_REMOTE_SOUND]
                ?: PulseLinkSettings().autoAllowRemoteSoundChange,
            assistantShortcutsDismissed = prefs[ASSISTANT_HINT_DISMISSED]
                ?: PulseLinkSettings().assistantShortcutsDismissed,
            emergencyProfile = prefs[EMERGENCY_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().emergencyProfile,
            checkInProfile = prefs[CHECKIN_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().checkInProfile,
            callSoundKey = prefs[CALL_SOUND] ?: PulseLinkSettings().callSoundKey,
            betaAgreementAccepted = prefs[BETA_AGREEMENT_ACCEPTED] ?: PulseLinkSettings().betaAgreementAccepted,
            betaAgreementVersion = prefs[BETA_AGREEMENT_VERSION] ?: PulseLinkSettings().betaAgreementVersion,
            autoCallAfterAlert = prefs[AUTO_CALL] ?: PulseLinkSettings().autoCallAfterAlert,
            proUnlocked = prefs[PRO_UNLOCKED] ?: PulseLinkSettings().proUnlocked,
            premiumUnlocked = prefs[PREMIUM_UNLOCKED] ?: PulseLinkSettings().premiumUnlocked,
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: PulseLinkSettings().onboardingComplete,
            deviceId = prefs[DEVICE_ID] ?: PulseLinkSettings().deviceId,
            ownerName = prefs[OWNER_NAME] ?: PulseLinkSettings().ownerName,
            isBetaTester = prefs[IS_BETA_TESTER] ?: PulseLinkSettings().isBetaTester,
            autoUpdateContactInfo = prefs[AUTO_UPDATE_CONTACT_INFO] ?: PulseLinkSettings().autoUpdateContactInfo,
            timeFormat = prefs[TIME_FORMAT]?.let { runCatching { com.pulselink.domain.model.TimeFormat.valueOf(it) }.getOrNull() }
                ?: PulseLinkSettings().timeFormat,
            themePreferences = prefs[THEME_PREFERENCES]?.let {
                runCatching { json.decodeFromString(com.pulselink.domain.model.ThemePreferences.serializer(), it) }.getOrNull()
            } ?: PulseLinkSettings().themePreferences,
            remoteWebAccessEnabled = prefs[REMOTE_WEB_ACCESS] ?: PulseLinkSettings().remoteWebAccessEnabled,
            privatePinHash = prefs[PRIVATE_PIN_HASH],
            privateThreadIds = prefs[PRIVATE_THREADS]?.let {
                runCatching { json.decodeFromString<List<Long>>(it) }.getOrNull()
            } ?: PulseLinkSettings().privateThreadIds,
            beaconLauncherEnabled = prefs[BEACON_LAUNCHER_ENABLED] ?: PulseLinkSettings().beaconLauncherEnabled
        )
    }

    override suspend fun update(transform: (PulseLinkSettings) -> PulseLinkSettings) {
        dataStore.edit { prefs ->
            val current = settingsValue(prefs)
            val updated = transform(current)
            prefs[PRIMARY_PHRASE] = updated.primaryPhrase
            prefs[SECONDARY_PHRASE] = updated.secondaryPhrase
            prefs[INCLUDE_LOCATION] = updated.includeLocation
            prefs[AUTO_ALLOW_REMOTE_SOUND] = updated.autoAllowRemoteSoundChange
            prefs[ASSISTANT_HINT_DISMISSED] = updated.assistantShortcutsDismissed
            prefs[EMERGENCY_PROFILE] = json.encodeToString(AlertProfile.serializer(), updated.emergencyProfile)
            prefs[CHECKIN_PROFILE] = json.encodeToString(AlertProfile.serializer(), updated.checkInProfile)
            updated.callSoundKey?.let { prefs[CALL_SOUND] = it } ?: prefs.remove(CALL_SOUND)
            prefs[BETA_AGREEMENT_ACCEPTED] = updated.betaAgreementAccepted
            updated.betaAgreementVersion?.let { prefs[BETA_AGREEMENT_VERSION] = it } ?: prefs.remove(BETA_AGREEMENT_VERSION)
            prefs[AUTO_CALL] = updated.autoCallAfterAlert
            prefs[PRO_UNLOCKED] = updated.proUnlocked
            prefs[PREMIUM_UNLOCKED] = updated.premiumUnlocked
            prefs[ONBOARDING_COMPLETE] = updated.onboardingComplete
            prefs[DEVICE_ID] = updated.deviceId
            prefs[OWNER_NAME] = updated.ownerName
            prefs[IS_BETA_TESTER] = updated.isBetaTester
            prefs[AUTO_UPDATE_CONTACT_INFO] = updated.autoUpdateContactInfo
            prefs[TIME_FORMAT] = updated.timeFormat.name
            prefs[THEME_PREFERENCES] = json.encodeToString(com.pulselink.domain.model.ThemePreferences.serializer(), updated.themePreferences)
            prefs[REMOTE_WEB_ACCESS] = updated.remoteWebAccessEnabled
            updated.privatePinHash?.let { prefs[PRIVATE_PIN_HASH] = it } ?: prefs.remove(PRIVATE_PIN_HASH)
            prefs[PRIVATE_THREADS] = json.encodeToString(updated.privateThreadIds)
            prefs[BEACON_LAUNCHER_ENABLED] = updated.beaconLauncherEnabled
        }
    }

    override suspend fun setProUnlocked(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PRO_UNLOCKED] = enabled
        }
    }

    override suspend fun setPremiumUnlocked(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PREMIUM_UNLOCKED] = enabled
        }
    }

    override suspend fun setOnboardingComplete() {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = true
        }
    }

    override suspend fun ensureDeviceId(): String {
        val existing = dataStore.data.map { it[DEVICE_ID] }.first()
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            prefs[DEVICE_ID] = generated
        }
        return generated
    }

    override suspend fun setOwnerName(name: String) {
        dataStore.edit { prefs ->
            prefs[OWNER_NAME] = name
        }
    }

    override suspend fun setAutoAllowRemoteSoundChange(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_ALLOW_REMOTE_SOUND] = enabled
        }
    }

    override suspend fun setBetaTesterStatus(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_BETA_TESTER] = enabled
        }
    }

    override suspend fun setAssistantShortcutsDismissed(dismissed: Boolean) {
        dataStore.edit { prefs ->
            prefs[ASSISTANT_HINT_DISMISSED] = dismissed
        }
    }

    override suspend fun setBetaAgreementAcceptance(version: String) {
        dataStore.edit { prefs ->
            prefs[BETA_AGREEMENT_ACCEPTED] = true
            prefs[BETA_AGREEMENT_VERSION] = version
        }
    }

    override suspend fun getLastWidgetCheckTimestamp(): Long {
        return dataStore.data.map { it[WIDGET_LAST_CHECK_TIMESTAMP] ?: 0L }.first()
    }

    override suspend fun updateLastWidgetCheckTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[WIDGET_LAST_CHECK_TIMESTAMP] = timestamp
        }
    }

    override suspend fun setEmergencyActive(active: Boolean) {
        dataStore.edit { prefs ->
            prefs[EMERGENCY_ACTIVE] = active
        }
    }

    override suspend fun isEmergencyActive(): Boolean {
        return dataStore.data.map { it[EMERGENCY_ACTIVE] ?: false }.first()
    }

    override suspend fun setLastKnownPhone(phone: String?) {
        dataStore.edit { prefs ->
            phone?.let { prefs[LAST_KNOWN_PHONE] = it } ?: prefs.remove(LAST_KNOWN_PHONE)
        }
    }

    override suspend fun getLastKnownPhone(): String? {
        return dataStore.data.map { it[LAST_KNOWN_PHONE] }.first()
    }

    override suspend fun setLastKnownEmail(email: String?) {
        dataStore.edit { prefs ->
            email?.let { prefs[LAST_KNOWN_EMAIL] = it } ?: prefs.remove(LAST_KNOWN_EMAIL)
        }
    }

    override suspend fun getLastKnownEmail(): String? {
        return dataStore.data.map { it[LAST_KNOWN_EMAIL] }.first()
    }

    override suspend fun setAutoUpdateContactInfo(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_UPDATE_CONTACT_INFO] = enabled
        }
    }

    override suspend fun setTimeFormat(format: com.pulselink.domain.model.TimeFormat) {
        dataStore.edit { prefs ->
            prefs[TIME_FORMAT] = format.name
        }
    }

    override suspend fun setThemePreferences(theme: com.pulselink.domain.model.ThemePreferences) {
        dataStore.edit { prefs ->
            prefs[THEME_PREFERENCES] = json.encodeToString(com.pulselink.domain.model.ThemePreferences.serializer(), theme)
        }
    }

    override suspend fun setRemoteWebAccessEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REMOTE_WEB_ACCESS] = enabled
        }
    }

    override suspend fun setPrivatePinHash(hash: String?) {
        dataStore.edit { prefs ->
            hash?.let { prefs[PRIVATE_PIN_HASH] = it } ?: prefs.remove(PRIVATE_PIN_HASH)
        }
    }

    override suspend fun setPrivateThreads(threadIds: List<Long>) {
        dataStore.edit { prefs ->
            prefs[PRIVATE_THREADS] = json.encodeToString(threadIds.distinct())
        }
    }

    override suspend fun setBeaconLauncherEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BEACON_LAUNCHER_ENABLED] = enabled
        }
    }

    private fun settingsValue(prefs: Preferences): PulseLinkSettings {
        return PulseLinkSettings(
            primaryPhrase = prefs[PRIMARY_PHRASE] ?: PulseLinkSettings().primaryPhrase,
            secondaryPhrase = prefs[SECONDARY_PHRASE] ?: PulseLinkSettings().secondaryPhrase,
            includeLocation = prefs[INCLUDE_LOCATION] ?: PulseLinkSettings().includeLocation,
            autoAllowRemoteSoundChange = prefs[AUTO_ALLOW_REMOTE_SOUND]
                ?: PulseLinkSettings().autoAllowRemoteSoundChange,
            assistantShortcutsDismissed = prefs[ASSISTANT_HINT_DISMISSED]
                ?: PulseLinkSettings().assistantShortcutsDismissed,
            emergencyProfile = prefs[EMERGENCY_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().emergencyProfile,
            checkInProfile = prefs[CHECKIN_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().checkInProfile,
            callSoundKey = prefs[CALL_SOUND] ?: PulseLinkSettings().callSoundKey,
            betaAgreementAccepted = prefs[BETA_AGREEMENT_ACCEPTED] ?: PulseLinkSettings().betaAgreementAccepted,
            betaAgreementVersion = prefs[BETA_AGREEMENT_VERSION] ?: PulseLinkSettings().betaAgreementVersion,
            autoCallAfterAlert = prefs[AUTO_CALL] ?: PulseLinkSettings().autoCallAfterAlert,
            proUnlocked = prefs[PRO_UNLOCKED] ?: PulseLinkSettings().proUnlocked,
            premiumUnlocked = prefs[PREMIUM_UNLOCKED] ?: PulseLinkSettings().premiumUnlocked,
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: PulseLinkSettings().onboardingComplete,
            deviceId = prefs[DEVICE_ID] ?: PulseLinkSettings().deviceId,
            ownerName = prefs[OWNER_NAME] ?: PulseLinkSettings().ownerName,
            isBetaTester = prefs[IS_BETA_TESTER] ?: PulseLinkSettings().isBetaTester,
            autoUpdateContactInfo = prefs[AUTO_UPDATE_CONTACT_INFO] ?: PulseLinkSettings().autoUpdateContactInfo,
            timeFormat = prefs[TIME_FORMAT]?.let { runCatching { com.pulselink.domain.model.TimeFormat.valueOf(it) }.getOrNull() }
                ?: PulseLinkSettings().timeFormat,
            themePreferences = prefs[THEME_PREFERENCES]?.let {
                runCatching { json.decodeFromString(com.pulselink.domain.model.ThemePreferences.serializer(), it) }.getOrNull()
            } ?: PulseLinkSettings().themePreferences,
            remoteWebAccessEnabled = prefs[REMOTE_WEB_ACCESS] ?: PulseLinkSettings().remoteWebAccessEnabled,
            privatePinHash = prefs[PRIVATE_PIN_HASH],
            privateThreadIds = prefs[PRIVATE_THREADS]?.let {
                runCatching { json.decodeFromString<List<Long>>(it) }.getOrNull()
            } ?: PulseLinkSettings().privateThreadIds,
            beaconLauncherEnabled = prefs[BEACON_LAUNCHER_ENABLED] ?: PulseLinkSettings().beaconLauncherEnabled
        )
    }

        override suspend fun setLastKnownPhone(phone: String?) {
                    dataStore.edit { prefs ->
                                if (phone == null) {
                                                    prefs.remove(LAST_KNOWN_PHONE)
                                                                } else {

                                                                                }
                                                                                        }
                                                                                            }

                                                                                                override suspend fun getLastKnownPhone(): String? =
                                                                                                        dataStore.data.first()[LAST_KNOWN_PHONE]

                                                                                                            override suspend fun setLastKnownEmail(email: String?) {
                                                                                                                        dataStore.edit { prefs ->
                                                                                                                                    if (email == null) {
                                                                                                                                                        prefs.remove(LAST_KNOWN_EMAIL)
                                                                                                                                                                    } else {
                                                                                                                                                                                        prefs[LAST_KNOWN_EMAIL] = email
                                                                                                                                                                                                    }
                                                                                                                                                                                                            }
                                                                                                                                                                                                                }

                                                                                                                                                                                                                    override suspend fun getLastKnownEmail(): String? =
                                                                                                                                                                                                                            dataStore.data.first()[LAST_KNOWN_EMAIL]
}

fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_NAME) }
    )
