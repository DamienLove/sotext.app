package com.pulselink.data.db

import androidx.room.TypeConverter
import com.pulselink.domain.model.MessageDirection

class Converters {
    @TypeConverter
    fun fromDirection(direction: MessageDirection): String = direction.name

    @TypeConverter
    fun toDirection(value: String): MessageDirection = MessageDirection.valueOf(value)

    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.joinToString("||")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
}
