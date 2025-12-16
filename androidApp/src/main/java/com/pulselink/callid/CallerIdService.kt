package com.pulselink.callid

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class CallerIdService @Inject constructor(
    providers: Set<@JvmSuppressWildcards CallerIdProvider>
) {
    private val orderedProviders: List<CallerIdProvider> = providers.sortedBy { it.priority }

    suspend fun lookup(rawNumber: String): CallerIdLookupResult? = withContext(Dispatchers.IO) {
        val normalized = rawNumber.filter { it.isDigit() || it == '+' }
        if (normalized.isBlank()) return@withContext null

        for (provider in orderedProviders) {
            val redacted = normalized.takeLast(4).padStart(normalized.length, '*')
            val result = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                provider.lookup(normalized)
            }
            if (result != null) {
                Log.i(TAG, "[${provider.providerName}] hit for $redacted -> ${result.summary}")
                return@withContext result
            } else {
                Log.w(TAG, "[${provider.providerName}] no result for $redacted")
            }
        }
        null
    }

    companion object {
        private const val TAG = "CallerIdService"
        private const val PROVIDER_TIMEOUT_MS = 3_000L
    }
}
