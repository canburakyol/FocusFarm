package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.ReflectionHistoryItem
import kotlinx.coroutines.flow.Flow

interface ReflectionRepository {
    fun observeRecentHistory(limit: Int = 30): Flow<List<ReflectionHistoryItem>>
    suspend fun upsertReflection(
        sessionId: Long,
        note: String,
        nowMillis: Long = System.currentTimeMillis(),
    )
}
