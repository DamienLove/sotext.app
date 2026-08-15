package com.sotext.domain.repository

import com.sotext.domain.model.ContactMessage
import com.sotext.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeForContact(contactId: Long): Flow<List<ContactMessage>>
    suspend fun record(message: ContactMessage)
    suspend fun updateMessageStatus(messageId: String, contactId: Long, status: MessageStatus)
    suspend fun clear(contactId: Long)
    suspend fun getUnreadEmergencyMessageCount(contactIds: List<Long>, since: Long): Int
}
