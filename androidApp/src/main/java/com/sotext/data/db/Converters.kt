package com.sotext.data.db

import androidx.room.TypeConverter
import com.sotext.domain.model.MessageDirection
import com.sotext.domain.model.MessageStatus
import com.sotext.domain.model.RecurrenceRule
import com.sotext.domain.model.ScheduledAttachment
import com.sotext.domain.model.ScheduledMessageStatus
import com.sotext.domain.model.ThemePreferences
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromThemePreferences(theme: ThemePreferences?): String? {
        return theme?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toThemePreferences(value: String?): ThemePreferences? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromDirection(direction: MessageDirection): String = direction.name

    @TypeConverter
    fun toDirection(value: String): MessageDirection = MessageDirection.valueOf(value)

    @TypeConverter
    fun fromStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.joinToString("||")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.split("||")?.filter { it.isNotBlank() } ?: emptyList()

    @TypeConverter
    fun fromScheduledAttachments(value: List<ScheduledAttachment>?): String? =
        // ScheduledMessage.attachments is a non-nullable Kotlin List, so Room generates a NOT
        // NULL column for it - an empty (but non-null) list must still serialize to something
        // ("[]"), never to SQL NULL, or every insert with no attachments violates that
        // constraint. Only a genuinely null `value` (this converter's signature is nullable for
        // reuse elsewhere) should ever produce a null column value.
        value?.let { Json.encodeToString(it) }

    @TypeConverter
    fun toScheduledAttachments(value: String?): List<ScheduledAttachment> =
        value?.let { runCatching { Json.decodeFromString<List<ScheduledAttachment>>(it) }.getOrNull() } ?: emptyList()

    @TypeConverter
    fun fromRecurrenceRule(value: RecurrenceRule?): String? =
        value?.let { Json.encodeToString(it) }

    @TypeConverter
    fun toRecurrenceRule(value: String?): RecurrenceRule? =
        value?.let { runCatching { Json.decodeFromString<RecurrenceRule>(it) }.getOrNull() }

    @TypeConverter
    fun fromScheduledMessageStatus(status: ScheduledMessageStatus): String = status.name

    @TypeConverter
    fun toScheduledMessageStatus(value: String): ScheduledMessageStatus =
        runCatching { ScheduledMessageStatus.valueOf(value) }.getOrDefault(ScheduledMessageStatus.FAILED)
}
