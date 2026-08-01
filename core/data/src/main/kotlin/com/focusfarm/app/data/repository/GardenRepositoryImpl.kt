package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.data.local.GardenEntryDao
import com.focusfarm.app.data.local.GardenEntryEntity
import com.focusfarm.app.data.local.GardenEntryWithSpecies
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.GardenEntry
import com.focusfarm.app.domain.GardenFilter
import com.focusfarm.app.domain.GardenPlantState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar

@Singleton
class GardenRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val gardenEntryDao: GardenEntryDao,
) : GardenRepository {
    override fun getAllSessions(): Flow<List<FocusSession>> =
        sessionDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override fun getRecentSessions(limit: Int): Flow<List<FocusSession>> =
        sessionDao.getRecentSessions(limit).map { list -> list.map { it.toDomain() } }

    override fun getSessionCount(): Flow<Int> =
        sessionDao.getSessionCount()

    override fun getTotalMinutes(): Flow<Int> =
        sessionDao.getTotalMinutes()

    override fun getSessionsSince(startOfWeek: Long): Flow<List<FocusSession>> =
        sessionDao.getSessionsSince(startOfWeek).map { list -> list.map { it.toDomain() } }

    override suspend fun saveSession(session: FocusSession): Long =
        sessionDao.insert(session.toEntity())

    override suspend fun getRecentSessionsSnapshot(limit: Int): List<FocusSession> =
        sessionDao.getRecentSessionsSnapshot(limit).map { it.toDomain() }

    override fun getGardenEntries(filter: GardenFilter, hideWithered: Boolean): Flow<List<GardenEntry>> {
        val sinceEpoch = getStartTimestampForFilter(filter)
        val flow = if (hideWithered) {
            if (sinceEpoch == 0L) {
                gardenEntryDao.observeAllExcludingWitheredWithSpecies()
            } else {
                gardenEntryDao.observeSinceExcludingWitheredWithSpecies(sinceEpoch)
            }
        } else {
            if (sinceEpoch == 0L) {
                gardenEntryDao.observeAllWithSpecies()
            } else {
                gardenEntryDao.observeSinceWithSpecies(sinceEpoch)
            }
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertGardenEntry(entry: GardenEntry) {
        gardenEntryDao.insert(entry.toEntity())
    }

    override fun getBloomedCount(): Flow<Int> = gardenEntryDao.observeBloomedCount()

    override fun getTotalGardenCount(): Flow<Int> = gardenEntryDao.observeTotalCount()

    private fun getStartTimestampForFilter(filter: GardenFilter): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return when (filter) {
            GardenFilter.TODAY -> calendar.timeInMillis
            GardenFilter.WEEKLY -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val diff = if (dayOfWeek == Calendar.SUNDAY) {
                    -6
                } else {
                    Calendar.MONDAY - dayOfWeek
                }
                calendar.add(Calendar.DAY_OF_WEEK, diff)
                calendar.timeInMillis
            }
            GardenFilter.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
            GardenFilter.ALL_TIME -> 0L
        }
    }
}

private fun GardenEntryWithSpecies.toDomain(): GardenEntry = GardenEntry(
    id = entry.id,
    sessionId = entry.sessionId,
    plantSpeciesId = entry.plantSpeciesId,
    state = runCatching { GardenPlantState.valueOf(entry.state) }.getOrDefault(GardenPlantState.BLOOMED),
    plantedAt = entry.plantedAt,
    focusMinutes = entry.focusMinutes,
    seedsEarned = entry.seedsEarned,
    tagId = entry.tagId,
    growthStageReached = entry.growthStageReached,
)

private fun GardenEntry.toEntity(): GardenEntryEntity = GardenEntryEntity(
    id = id,
    sessionId = sessionId,
    plantSpeciesId = plantSpeciesId,
    state = state.name,
    plantedAt = plantedAt,
    focusMinutes = focusMinutes,
    seedsEarned = seedsEarned,
    tagId = tagId,
    growthStageReached = growthStageReached,
)

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
