package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecoverySessionStatus { OFFERED, RUNNING, COMPLETED }

@Entity(tableName = "recovery_sessions")
data class RecoverySessionEntity(
    @PrimaryKey val activeSessionId: String,
    val plantId: String,
    val suggestedMinutes: Int,
    val status: RecoverySessionStatus,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?,
)
