package com.pulselink.beacon.data.scheduled

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {
    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' ORDER BY scheduledTimeMillis ASC")
    fun getAllPending(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' AND scheduledTimeMillis <= :currentTime")
    suspend fun getReadyToSend(currentTime: Long): List<ScheduledMessage>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun getById(id: Long): ScheduledMessage?

    @Insert
    suspend fun insert(message: ScheduledMessage): Long

    @Update
    suspend fun update(message: ScheduledMessage)

    @Delete
    suspend fun delete(message: ScheduledMessage)
}
