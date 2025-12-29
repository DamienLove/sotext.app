package com.pulselink.beacon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.inboxDataStore by preferencesDataStore(name = "beacon_inbox_prefs")

class InboxPreferencesRepository(private val context: Context) {

    private val pinnedKey = stringPreferencesKey("pinned_threads")
    private val archivedKey = stringPreferencesKey("archived_threads")

    val flow: Flow<InboxState> = context.inboxDataStore.data.map { prefs ->
        val pinned = decodeSet(prefs[pinnedKey])
        val archived = decodeSet(prefs[archivedKey])
        InboxState(pinnedThreadIds = pinned, archivedThreadIds = archived)
    }

    suspend fun togglePin(threadId: Long) {
        context.inboxDataStore.edit { prefs ->
            val current = decodeSet(prefs[pinnedKey]).toMutableSet()
            if (current.contains(threadId)) {
                current.remove(threadId)
            } else {
                current.add(threadId)
            }
            prefs[pinnedKey] = encodeSet(current)
        }
    }

    suspend fun toggleArchive(threadId: Long) {
        context.inboxDataStore.edit { prefs ->
            val current = decodeSet(prefs[archivedKey]).toMutableSet()
            if (current.contains(threadId)) {
                current.remove(threadId)
            } else {
                current.add(threadId)
            }
            prefs[archivedKey] = encodeSet(current)
        }
    }

    private fun decodeSet(raw: String?): Set<Long> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun encodeSet(set: Set<Long>): String {
        return set.joinToString(",")
    }
}

data class InboxState(
    val pinnedThreadIds: Set<Long> = emptySet(),
    val archivedThreadIds: Set<Long> = emptySet()
)
