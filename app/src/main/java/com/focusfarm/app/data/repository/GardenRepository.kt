package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.domain.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GardenRepository @Inject constructor(
    private val sessionDao: SessionDao,
) {
    fun getAllSessions(): Flow<List<FocusSession>> =
        sessionDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    fun getRecentSessions(limit: Int = 5): Flow<List<FocusSession>> =
        sessionDao.getRecentSessions(limit).map { list -> list.map { it.toDomain() } }

    fun getSessionCount(): Flow<Int> =
        sessionDao.getSessionCount()

    fun getTotalMinutes(): Flow<Int> =
        sessionDao.getTotalMinutes()

    fun getSessionsSince(startOfWeek: Long): Flow<List<FocusSession>> =
        sessionDao.getSessionsSince(startOfWeek).map { list -> list.map { it.toDomain() } }

    suspend fun saveSession(session: FocusSession): Long =
        sessionDao.insert(session.toEntity())
}

private fun SessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    plantId = plantId,
    plantName = plantName,
    plantEmoji = plantEmoji,
    durationMinutes = durationMinutes,
    completedAt = completedAt,
)

private fun FocusSession.toEntity(): SessionEntity = SessionEntity(
    id = id,
    plantId = plantId,
    plantName = plantName,
    plantEmoji = plantEmoji,
    durationMinutes = durationMinutes,
    completedAt = completedAt,
)
