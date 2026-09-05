package com.mtpnda.biocue.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stress_events")
data class StressEventEntity(
    @PrimaryKey
    val id: String, // UUID string
    val startTime: Long,
    val endTime: Long?,
    val triggerType: String, // "AUTO" or "MANUAL"
    val label: String,       // "OVERWHELMED", "EXCITED", "ACTIVE", "NONE"
    val isResolved: Boolean  // True if label is updated by caregiver or wearer
)
