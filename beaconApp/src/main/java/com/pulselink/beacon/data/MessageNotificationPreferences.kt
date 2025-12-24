package com.pulselink.beacon.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.messageSettingsDataStore by preferencesDataStore(name = "beacon_message_settings")

data class MessageNotificationPrefs(
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val contactOverrides: Map<String, String> = emptyMap()
)

class MessageNotificationPreferences(private val context: Context) {
    private val globalSoundKey = stringPreferencesKey("message_sound_uri")
    private val vibrateKey = booleanPreferencesKey("message_vibrate")
    private val overridesKey = stringPreferencesKey("message_sound_overrides")

    val flow: Flow<MessageNotificationPrefs> = context.messageSettingsDataStore.data.map { prefs ->
        MessageNotificationPrefs(
            soundUri = prefs[globalSoundKey],
            vibrate = prefs[vibrateKey] ?: true,
            contactOverrides = decodeOverrides(prefs[overridesKey])
        )
    }

    suspend fun getConfig(): MessageNotificationPrefs = flow.first()

    suspend fun setGlobalSound(uri: String?) {
        context.messageSettingsDataStore.edit { prefs ->
            uri?.let { prefs[globalSoundKey] = it } ?: prefs.remove(globalSoundKey)
        }
    }

    suspend fun setVibrate(enabled: Boolean) {
        context.messageSettingsDataStore.edit { prefs ->
            prefs[vibrateKey] = enabled
        }
    }

    suspend fun setContactSound(address: String, uri: String?) {
        context.messageSettingsDataStore.edit { prefs ->
            val current = decodeOverrides(prefs[overridesKey]).toMutableMap()
            if (uri.isNullOrBlank()) {
                current.remove(address)
            } else {
                current[address] = uri
            }
            prefs[overridesKey] = encodeOverrides(current)
        }
    }

    private fun decodeOverrides(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split("|")
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = Uri.decode(pair.substring(0, idx))
                val value = Uri.decode(pair.substring(idx + 1))
                if (key.isBlank() || value.isBlank()) null else key to value
            }
            .toMap()
    }

    private fun encodeOverrides(map: Map<String, String>): String {
        if (map.isEmpty()) return ""
        return map.entries.joinToString("|") { entry ->
            "${Uri.encode(entry.key)}=${Uri.encode(entry.value)}"
        }
    }
}
