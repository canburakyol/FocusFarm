package com.focusfarm.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_reflections")
data class SessionReflectionEntity(
    @PrimaryKey
    val sessionId: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
