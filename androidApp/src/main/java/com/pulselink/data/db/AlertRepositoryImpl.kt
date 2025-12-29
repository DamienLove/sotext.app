package com.pulselink.data.db

import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertEventDao: AlertEventDao
) : AlertRepository {
    override fun observeRecent(limit: Int): Flow<List<AlertEvent>> = alertEventDao.observeRecent(limit)

    override suspend fun record(event: AlertEvent) {
        alertEventDao.insert(event)
    }
}
