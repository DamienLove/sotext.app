package com.sotext.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_threads")
data class PinnedThread(
    @PrimaryKey val threadId: Long
)
