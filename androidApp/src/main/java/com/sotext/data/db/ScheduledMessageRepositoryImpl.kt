package com.sotext.data.db

import com.sotext.domain.model.ScheduledMessage
import com.sotext.domain.repository.ScheduledMessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledMessageRepositoryImpl @Inject constructor(
    private val scheduledMessageDao: ScheduledMessageDao
) : ScheduledMessageRepository {

    override suspend fun insert(message: ScheduledMessage): Long = scheduledMessageDao.insert(message)

    override suspend fun update(message: ScheduledMessage) {
        // Any content edit is a fresh local change the cloud copy (if any) no longer reflects.
        scheduledMessageDao.update(message.copy(updatedAt = System.currentTimeMillis(), syncedToCloud = false))
    }

    override suspend fun getById(id: Long): ScheduledMessage? = scheduledMessageDao.getById(id)

    override suspend fun getByOccurrenceKey(occurrenceKey: String): ScheduledMessage? =
        scheduledMessageDao.getByOccurrenceKey(occurrenceKey)

    override suspend fun getBySeriesAndTime(seriesId: String, scheduledForUtcMillis: Long): ScheduledMessage? =
        scheduledMessageDao.getBySeriesAndTime(seriesId, scheduledForUtcMillis)

    override fun observeForThread(threadId: Long): Flow<List<ScheduledMessage>> =
        scheduledMessageDao.observeForThread(threadId)

    override fun observeForAddress(address: String): Flow<List<ScheduledMessage>> =
        scheduledMessageDao.observeForAddress(address)

    override fun observeUpcoming(): Flow<List<ScheduledMessage>> = scheduledMessageDao.observeUpcoming()

    override suspend fun getDueForDispatch(nowUtcMillis: Long): List<ScheduledMessage> =
        scheduledMessageDao.getDueForDispatch(nowUtcMillis)

    override suspend fun getRetryableFailed(maxRetries: Int, retryEligibleBeforeMillis: Long): List<ScheduledMessage> =
        scheduledMessageDao.getRetryableFailed(maxRetries, retryEligibleBeforeMillis)

    override suspend fun getStaleProcessing(staleBeforeMillis: Long): List<ScheduledMessage> =
        scheduledMessageDao.getStaleProcessing(staleBeforeMillis)

    override suspend fun getAllScheduled(): List<ScheduledMessage> = scheduledMessageDao.getAllScheduled()

    override suspend fun getAllActive(): List<ScheduledMessage> = scheduledMessageDao.getAllActive()

    override suspend fun getUnsyncedToCloud(): List<ScheduledMessage> = scheduledMessageDao.getUnsyncedToCloud()

    override suspend fun markSyncedToCloud(id: Long, cloudDocId: String) =
        scheduledMessageDao.markSyncedToCloud(id, cloudDocId)

    override suspend fun claimForProcessing(id: Long): Boolean =
        scheduledMessageDao.claimForProcessing(id, System.currentTimeMillis()) > 0

    override suspend fun markSent(id: Long, sentMessageId: Long?) {
        scheduledMessageDao.markSent(id, sentMessageId, System.currentTimeMillis())
    }

    override suspend fun markFailed(id: Long, error: String) {
        scheduledMessageDao.markFailed(id, error, System.currentTimeMillis())
    }

    override suspend fun cancel(id: Long): Boolean =
        scheduledMessageDao.cancel(id, System.currentTimeMillis()) > 0

    override suspend fun reclaimStale(id: Long): Boolean =
        scheduledMessageDao.reclaimStale(id, System.currentTimeMillis()) > 0

    override suspend fun deleteById(id: Long) = scheduledMessageDao.deleteById(id)
}
