package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["completedAt"])],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: String,
    val plantName: String,
    val plantEmoji: String,
    val durationMinutes: Int,
    val completedAt: Long, // epoch millis
)
