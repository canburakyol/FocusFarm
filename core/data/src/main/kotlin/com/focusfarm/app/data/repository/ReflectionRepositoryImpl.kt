package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.ReflectionDao
import com.focusfarm.app.data.local.ReflectionHistoryItem
import com.focusfarm.app.data.local.SessionReflectionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ReflectionRepositoryImpl @Inject constructor(
    private val reflectionDao: ReflectionDao,
) : ReflectionRepository {
    override fun observeRecentHistory(limit: Int): Flow<List<ReflectionHistoryItem>> =
        reflectionDao.observeRecentReflectionHistory(limit)

    override suspend fun upsertReflection(
        sessionId: Long,
        note: String,
        nowMillis: Long,
    ) {
        val trimmed = note.trim()
        if (trimmed.isEmpty()) {
            reflectionDao.deleteReflection(sessionId)
            return
        }
        val existing = reflectionDao.getReflection(sessionId)
        reflectionDao.upsertReflection(
            SessionReflectionEntity(
                sessionId = sessionId,
                note = trimmed,
                createdAt = existing?.createdAt ?: nowMillis,
                updatedAt = nowMillis,
            )
        )
    }
}
