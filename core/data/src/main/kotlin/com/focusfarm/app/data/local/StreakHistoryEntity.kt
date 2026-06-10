package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streak_history",
    indices = [Index(value = ["dateKey"], unique = true)],
)
data class StreakHistoryEntity(
    @PrimaryKey
    val dateKey: String,
    val streakCount: Int,
    val sessionsCompleted: Int,
    val minutesFocused: Int,
    val recordedAt: Long,
)
