package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mission_progress")
data class MissionProgressEntity(
    @PrimaryKey
    val missionId: String,
    val currentValue: Int,
    val completedAt: Long?,
    val claimedAt: Long?,
)
