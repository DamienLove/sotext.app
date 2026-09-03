package com.sotext.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A message the user has asked SoText to send later instead of immediately, via the existing
 * SMS/MMS pipeline ([com.sotext.data.sms.SmsSender]/[com.sotext.data.sms.SmsStore]). Room is the
 * on-device source of truth (so scheduling/editing/cancelling works fully offline for every
 * user); when the owning user has `remoteWebAccessEnabled` on, rows are additionally mirrored to
 * Firestore (`users/{uid}/scheduledMessages/{occurrenceKey}`) by
 * `ScheduledMessageSyncService` for cross-device/web access.
 *
 * See [ScheduledMessageStatus] for the state machine and
 * `com.sotext.data.scheduled.ScheduledMessageDispatcher` for the single place that mutates it.
 */
@Entity(
    tableName = "scheduled_messages",
    indices = [
        Index(value = ["status", "scheduledForUtcMillis"], name = "index_scheduled_messages_status_time"),
        Index(value = ["occurrenceKey"], name = "index_scheduled_messages_occurrenceKey", unique = true),
        Index(value = ["threadId"], name = "index_scheduled_messages_threadId"),
        Index(value = ["seriesId"], name = "index_scheduled_messages_seriesId")
    ]
)
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Shared by every occurrence spawned from the same recurring schedule; equal to [occurrenceKey] for a one-off. */
    val seriesId: String = UUID.randomUUID().toString(),
    /**
     * Unique per occurrence. This is the row's real identity for idempotency purposes: it carries
     * a UNIQUE index in Room and doubles as the Firestore document id, so a retried "spawn the
     * next occurrence" can never insert the same occurrence twice.
     */
    val occurrenceKey: String = UUID.randomUUID().toString(),
    /** Telephony thread id, resolved/backfilled at send time via getOrCreateThreadId if null. */
    val threadId: Long? = null,
    /** Destination address(es), ';'-delimited for a broadcast — mirrors SmsThreadViewModel.sendMessage's own split. */
    val address: String,
    val body: String,
    val attachments: List<ScheduledAttachment> = emptyList(),
    /** null = the device's own default line. */
    val lineId: String? = null,
    val scheduledForUtcMillis: Long,
    /** IANA zone id captured at creation time, used for recurrence math and for display. */
    val timezoneId: String,
    val recurrenceRule: RecurrenceRule? = null,
    val status: ScheduledMessageStatus = ScheduledMessageStatus.SCHEDULED,
    val retryCount: Int = 0,
    val lastError: String? = null,
    /** Telephony row id of the resulting sent message, once known. */
    val sentMessageId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Set when claimed via the SCHEDULED->PROCESSING CAS; used to reclaim a row stuck by a crash mid-send. */
    val processingStartedAt: Long? = null,
    val syncedToCloud: Boolean = false,
    val cloudDocId: String? = null
)

enum class ScheduledMessageStatus {
    SCHEDULED,
    PROCESSING,
    SENT,
    FAILED,
    CANCELLED
}

@Serializable
data class ScheduledAttachment(
    val path: String,
    val mimeType: String,
    val displayName: String
)

@Serializable
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    /** Every N units (every 2 weeks, every 3 months, ...). */
    val interval: Int = 1,
    /** ISO-8601 weekday numbers, 1=Monday..7=Sunday. Used by WEEKLY/CUSTOM. */
    val daysOfWeek: Set<Int> = emptySet(),
    /** Used by MONTHLY; day-of-month is clamped to the target month's length. */
    val dayOfMonth: Int? = null,
    /** null = recurs indefinitely. */
    val endDateUtcMillis: Long? = null,
    /** Alternative end condition to [endDateUtcMillis]. */
    val occurrenceCount: Int? = null
)

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }
