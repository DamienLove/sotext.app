package com.pulselink.domain.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "contact_messages")
data class ContactMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String = UUID.randomUUID().toString(),
    val contactId: Long,
    val body: String,
    val direction: MessageDirection,
    val timestamp: Long = System.currentTimeMillis(),
    val overrideSucceeded: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageDirection {
    INBOUND,
    OUTBOUND
}

@get:Ignore
val ContactMessage.isSent: Boolean
    get() = direction == MessageDirection.OUTBOUND
