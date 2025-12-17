package com.pulselink.data.link

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.MessageChannel
import com.pulselink.domain.repository.ContactRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class MessageDeliveryTracker @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val contactRepository: ContactRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stats = ConcurrentHashMap<String, ChannelStats>()

    // Key: "contactId|channel" -> timestamp
    private val lastAttemptTime = ConcurrentHashMap<String, Long>()

    init {
        // Periodic sync to Firestore
        scope.launch {
            while (true) {
                delay(SYNC_INTERVAL_MS)
                syncStats()
            }
        }
    }

    fun recordAttempt(contactId: Long, channel: MessageChannel) {
        val key = "$contactId|${channel.name}"
        lastAttemptTime[key] = System.currentTimeMillis()
        getStats(channel).attempts.incrementAndGet()
    }

    suspend fun recordSuccess(contactId: Long, channel: MessageChannel) {
        val key = "$contactId|${channel.name}"
        lastAttemptTime.remove(key)
        getStats(channel).successes.incrementAndGet()

        // Update contact's last successful channel
        val contact = contactRepository.getContact(contactId)
        if (contact != null && contact.lastSuccessfulChannel != channel) {
            contactRepository.upsert(contact.copy(lastSuccessfulChannel = channel))
        }
    }

    fun recordFailure(contactId: Long, channel: MessageChannel) {
        val key = "$contactId|${channel.name}"
        lastAttemptTime.remove(key)
        getStats(channel).failures.incrementAndGet()
    }

    private fun getStats(channel: MessageChannel): ChannelStats {
        return stats.computeIfAbsent(channel.name) { ChannelStats() }
    }

    private fun syncStats() {
        if (stats.isEmpty()) return

        val batch = firestore.batch()
        val docRef = firestore.collection(COLLECTION_ANALYTICS).document("delivery_stats")

        val data = stats.entries.associate { (channel, stat) ->
            channel to mapOf(
                "attempts" to stat.attempts.get(),
                "successes" to stat.successes.get(),
                "failures" to stat.failures.get()
            )
        }

        // We use set with merge to avoid overwriting other fields
        batch.set(docRef, mapOf("channels" to data), com.google.firebase.firestore.SetOptions.merge())

        batch.commit().addOnFailureListener {
            Log.w(TAG, "Failed to sync delivery stats", it)
        }
    }

    data class ChannelStats(
        val attempts: java.util.concurrent.atomic.AtomicLong = java.util.concurrent.atomic.AtomicLong(0),
        val successes: java.util.concurrent.atomic.AtomicLong = java.util.concurrent.atomic.AtomicLong(0),
        val failures: java.util.concurrent.atomic.AtomicLong = java.util.concurrent.atomic.AtomicLong(0)
    )

    companion object {
        private const val TAG = "MessageDeliveryTracker"
        private const val COLLECTION_ANALYTICS = "analytics"
        private const val SYNC_INTERVAL_MS = 300_000L // 5 minutes
    }
}
