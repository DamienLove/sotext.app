package com.pulselink.beacon.data.scheduled

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_messages",
    indices = [Index(value = ["scheduledTimeMillis", "status"])]
)
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val body: String,
    val scheduledTimeMillis: Long,
    val status: MessageStatus = MessageStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MessageStatus {
    PENDING,
    SENT,
    FAILED
}
