package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.SessionStatus

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["completedAt"]),
        Index(value = ["status", "completedAt"])
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: String,
    val plantName: String,
    val plantEmoji: String,
    val durationMinutes: Int,
    val completedAt: Long, // epoch millis
    val selectedTag: FocusTag = FocusTag.OTHER,
    val status: SessionStatus = SessionStatus.COMPLETED,
    val failReason: String? = null,
)
