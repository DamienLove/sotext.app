package com.pulselink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_events")
data class AlertEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val triggeredBy: String,
    val tier: EscalationTier,
    val contactCount: Int,
    val sentSms: Boolean,
    val sharedLocation: Boolean
)
