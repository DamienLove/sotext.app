package com.sotext.domain.repository

import com.sotext.domain.model.ScheduledMessage
import kotlinx.coroutines.flow.Flow

/**
 * Single point every UI/worker/service talks to for scheduled-message persistence. Wraps
 * [com.sotext.data.db.ScheduledMessageDao] so the CAS/idempotency semantics documented on the DAO
 * live in exactly one place rather than being re-implemented per caller.
 */
interface ScheduledMessageRepository {
    suspend fun insert(message: ScheduledMessage): Long
    suspend fun update(message: ScheduledMessage)
    suspend fun getById(id: Long): ScheduledMessage?
    suspend fun getByOccurrenceKey(occurrenceKey: String): ScheduledMessage?
    suspend fun getBySeriesAndTime(seriesId: String, scheduledForUtcMillis: Long): ScheduledMessage?
    fun observeForThread(threadId: Long): Flow<List<ScheduledMessage>>
    fun observeForAddress(address: String): Flow<List<ScheduledMessage>>
    fun observeUpcoming(): Flow<List<ScheduledMessage>>
    suspend fun getDueForDispatch(nowUtcMillis: Long): List<ScheduledMessage>
    suspend fun getRetryableFailed(maxRetries: Int, retryEligibleBeforeMillis: Long): List<ScheduledMessage>
    suspend fun getStaleProcessing(staleBeforeMillis: Long): List<ScheduledMessage>
    suspend fun getAllScheduled(): List<ScheduledMessage>
    suspend fun getAllActive(): List<ScheduledMessage>
    suspend fun getUnsyncedToCloud(): List<ScheduledMessage>
    suspend fun markSyncedToCloud(id: Long, cloudDocId: String)
    suspend fun claimForProcessing(id: Long): Boolean
    suspend fun markSent(id: Long, sentMessageId: Long?)
    suspend fun markFailed(id: Long, error: String)
    suspend fun cancel(id: Long): Boolean
    suspend fun reclaimStale(id: Long): Boolean
    suspend fun deleteById(id: Long)
}
