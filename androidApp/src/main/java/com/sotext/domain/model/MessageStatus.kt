package com.sotext.domain.model

/**
 * Status of a message in its lifecycle
 * - SENDING: Message is being sent (local-only optimistic state)
 * - SENT: Message has been successfully written to Firestore
 * - DELIVERED: Message has been delivered to recipient's device (FCM received)
 * - READ: Message has been viewed by the recipient
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ;

    companion object {
        fun fromString(status: String?): MessageStatus {
            return when (status?.uppercase()) {
                "SENDING" -> SENDING
                "SENT" -> SENT
                "DELIVERED" -> DELIVERED
                "READ" -> READ
                else -> SENT // Default to SENT for backward compatibility
            }
        }
    }

    override fun toString(): String {
        return name
    }
}