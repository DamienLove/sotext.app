package com.pulselink.beacon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "beacon_theme_prefs")

class ThemePreferencesRepository(private val context: Context) {

    private val globalKey = stringPreferencesKey("global_theme")
    private val contactsKey = stringPreferencesKey("contact_themes")

    val flow: Flow<ThemeState> = context.themeDataStore.data.map { prefs ->
        val global = ThemePalette.decode(prefs[globalKey])
        val contacts = decodeContacts(prefs[contactsKey])
        ThemeState(global = global, contactThemes = contacts)
    }

    suspend fun saveGlobal(theme: ThemePalette) {
        context.themeDataStore.edit { prefs ->
            prefs[globalKey] = theme.encode()
        }
    }

    suspend fun saveContact(address: String, theme: ThemePalette?) {
        context.themeDataStore.edit { prefs ->
            val current = decodeContacts(prefs[contactsKey]).toMutableMap()
            if (theme == null) {
                current.remove(address)
            } else {
                current[address] = theme
            }
            prefs[contactsKey] = encodeContacts(current)
        }
    }

    private fun decodeContacts(raw: String?): Map<String, ThemePalette> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";;").mapNotNull { entry ->
            val parts = entry.split("::")
            val address = parts.getOrNull(0)
            val payload = parts.getOrNull(1)
            if (address.isNullOrBlank() || payload.isNullOrBlank()) null
            else address to ThemePalette.decode(payload)
        }.toMap()
    }

    private fun encodeContacts(map: Map<String, ThemePalette>): String {
        return map.entries.joinToString(";;") { (addr, theme) ->
            "$addr::${theme.encode()}"
        }
    }
}
