package com.sotext.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archived_threads")
data class ArchivedThread(
    @PrimaryKey val threadId: Long,
    val archivedAt: Long
)
