package com.pulselink.data.db

import androidx.room.TypeConverter
import com.pulselink.domain.model.MessageDirection
import com.pulselink.domain.model.ThemePreferences
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
    fun fromStringList(list: List<String>?): String? =
        list?.joinToString("||")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
}
