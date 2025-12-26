package com.pulselink.callid

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class CallerIdCacheRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    suspend fun lookupUserMapping(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        if (FirebaseAuth.getInstance().currentUser == null) return@withContext null
        val cached = memoryCache[cacheKey(USER_COLLECTION, phoneNumber)]
        if (cached != null && !cached.isExpired()) return@withContext cached.result
        val snapshot = runCatching {
            firestore.collection(USER_COLLECTION)
                .document(phoneNumber)
                .get()
                .await()
        }.getOrNull() ?: return@withContext null
        if (!snapshot.exists()) return@withContext null
        val displayName = snapshot.getString("displayName")?.trim().orEmpty()
        if (displayName.isBlank()) return@withContext null
        val summary = buildString {
            append(phoneNumber)
            append(" | ")
            append(displayName)
        }
        val result = CallerIdLookupResult(
            number = phoneNumber,
            carrier = null,
            location = null,
            lineType = null,
            valid = true,
            spamScore = null,
            isLikelySpam = false,
            source = "pulselink",
            summary = summary
        )
        memoryCache[cacheKey(USER_COLLECTION, phoneNumber)] = CacheEntry(result, System.currentTimeMillis() + TTL_MS)
        result
    }

    suspend fun getCached(phoneNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        if (FirebaseAuth.getInstance().currentUser == null) return@withContext null
        val cached = memoryCache[cacheKey(CACHE_COLLECTION, phoneNumber)]
        if (cached != null && !cached.isExpired()) return@withContext cached.result
        val snapshot = runCatching {
            firestore.collection(CACHE_COLLECTION)
                .document(phoneNumber)
                .get()
                .await()
        }.getOrNull() ?: return@withContext null
        if (!snapshot.exists()) return@withContext null
        val data = snapshot.data ?: return@withContext null
        val cachedAt = (data["updatedAtEpochMs"] as? Number)?.toLong()
            ?: snapshot.getTimestamp("updatedAt")?.toDate()?.time
            ?: return@withContext null
        if (System.currentTimeMillis() - cachedAt > TTL_MS) return@withContext null
        val result = decodeResult(data) ?: return@withContext null
        memoryCache[cacheKey(CACHE_COLLECTION, phoneNumber)] = CacheEntry(result, cachedAt + TTL_MS)
        result
    }

    suspend fun store(phoneNumber: String, result: CallerIdLookupResult) = withContext(Dispatchers.IO) {
        if (FirebaseAuth.getInstance().currentUser == null) return@withContext
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "number" to result.number,
            "carrier" to result.carrier,
            "location" to result.location,
            "lineType" to result.lineType,
            "valid" to result.valid,
            "spamScore" to result.spamScore,
            "isLikelySpam" to result.isLikelySpam,
            "source" to result.source,
            "summary" to result.summary,
            "updatedAt" to FieldValue.serverTimestamp(),
            "updatedAtEpochMs" to now,
            "expiresAt" to Timestamp((now + TTL_MS) / 1000, 0)
        )
        runCatching {
            firestore.collection(CACHE_COLLECTION)
                .document(phoneNumber)
                .set(payload)
                .await()
        }
        memoryCache[cacheKey(CACHE_COLLECTION, phoneNumber)] = CacheEntry(result, now + TTL_MS)
    }

    private fun decodeResult(data: Map<String, Any?>): CallerIdLookupResult? {
        val number = data["number"] as? String ?: return null
        val summary = data["summary"] as? String ?: return null
        val valid = data["valid"] as? Boolean ?: false
        val isLikelySpam = data["isLikelySpam"] as? Boolean ?: false
        return CallerIdLookupResult(
            number = number,
            carrier = data["carrier"] as? String,
            location = data["location"] as? String,
            lineType = data["lineType"] as? String,
            valid = valid,
            spamScore = (data["spamScore"] as? Number)?.toInt(),
            isLikelySpam = isLikelySpam,
            source = data["source"] as? String ?: "cache",
            summary = summary
        )
    }

    private fun cacheKey(collection: String, phoneNumber: String) =
        "$collection::$phoneNumber"

    private data class CacheEntry(
        val result: CallerIdLookupResult,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }

    companion object {
        private const val CACHE_COLLECTION = "callerIdCache"
        private const val USER_COLLECTION = "users_by_phone"
        private const val TTL_MS = 90L * 24L * 60L * 60L * 1000L
    }
}
