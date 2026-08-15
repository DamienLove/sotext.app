package com.sotext.callid

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight monthly usage tracker to prevent exceeding free-tier caps.
 * Tracks attempts per providerName and resets counts on month rollover.
 */
@Singleton
class CallerIdUsageLimiter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun tryConsume(providerName: String, monthlyCap: Int): Boolean {
        if (monthlyCap <= 0) return false
        synchronized(this) {
            val period = currentPeriod()
            val lastPeriod = prefs.getString(KEY_PERIOD, null)
            if (lastPeriod != period) {
                prefs.edit { clear(); putString(KEY_PERIOD, period) }
            }
            val key = countKey(providerName)
            val count = prefs.getInt(key, 0)
            if (count >= monthlyCap) return false
            prefs.edit { putInt(key, count + 1) }
            return true
        }
    }

    private fun currentPeriod(): String {
        val now = LocalDate.now()
        return "${now.year}-${now.monthValue}"
    }

    private fun countKey(providerName: String) = "count_$providerName"

    companion object {
        private const val PREFS_NAME = "caller_id_usage"
        private const val KEY_PERIOD = "period"
    }
}
