package com.focusfarm.app.domain

data class FocusSession(
    val id: Long = 0,
    val plantId: String,
    val plantName: String,
    val plantEmoji: String,
    val durationMinutes: Int,
    val completedAt: Long,
)
