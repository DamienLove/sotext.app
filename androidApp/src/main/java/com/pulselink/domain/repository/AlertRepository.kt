package com.pulselink.domain.repository

import com.pulselink.domain.model.AlertEvent
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun observeRecent(limit: Int = 25): Flow<List<AlertEvent>>
    suspend fun record(event: AlertEvent)
    fun observeUnreadCount(): Flow<Int>
    suspend fun markAsRead(ids: List<Long>)
}
