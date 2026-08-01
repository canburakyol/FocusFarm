package com.focusfarm.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.data.local.UserInventoryDao
import kotlin.math.floor
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.SessionStatus
import com.focusfarm.app.notifications.FocusReminderScheduler
import com.focusfarm.app.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class CompleteSessionParams(
    val activeSessionId: String? = null,
    val plantId: String,
    val plantName: String,
    val plantEmoji: String,
    val durationMinutes: Int,
    val completedAt: Long? = null,
    val rewardMultiplier: Float = 1f,
    val selectedTag: FocusTag = FocusTag.OTHER,
    val status: SessionStatus = SessionStatus.COMPLETED,
    val failReason: String? = null,
)

data class CompleteSessionResult(
    val sessionId: Long,
    val grantedSeeds: Int,
    val baseSeeds: Int,
    val wasBoosted: Boolean,
    val reminderRescheduled: Boolean,
    val grantedWaterDroplets: Int = 0,
    val grantedExperience: Int = 0,
)

@Singleton
class CompleteSessionUseCase @Inject constructor(
    private val database: AppDatabase,
    private val activeSessionDao: ActiveSessionDao,
    private val recoverySessionDao: RecoverySessionDao,
    private val localCohortTracker: LocalCohortTracker,
    private val sessionDao: SessionDao,
    private val progressRepository: ProgressRepository,
    private val userInventoryDao: UserInventoryDao,
    private val streakManager: StreakManager,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(params: CompleteSessionParams): CompleteSessionResult {
        require(params.durationMinutes >= 0) { "durationMinutes must be non-negative" }
        require(params.rewardMultiplier.isFinite() && params.rewardMultiplier >= 1f) {
            "rewardMultiplier must be finite and at least 1"
        }
        val completedAt = params.completedAt ?: clock.nowMillis()

        if (params.status == SessionStatus.FAILED || params.status == SessionStatus.CANCELLED) {
            val failedSessionId = database.withTransaction {
                val id = sessionDao.insert(
                    SessionEntity(
                        plantId = params.plantId,
                        plantName = if (params.status == SessionStatus.FAILED) "Solmuş Bitki" else "İptal Edilmiş Oturum",
                        plantEmoji = if (params.status == SessionStatus.FAILED) "🥀" else "⏹️",
                        durationMinutes = params.durationMinutes,
                        completedAt = completedAt,
                        selectedTag = params.selectedTag,
                        status = params.status,
                        failReason = params.failReason ?: "manual_abandon",
                    )
                )
                params.activeSessionId?.let { activeSessionId ->
                    activeSessionDao.delete(activeSessionId)
                }
                database.gardenEntryDao().insert(
                    com.focusfarm.app.data.local.GardenEntryEntity(
                        id = "ge-$id",
                        sessionId = id,
                        plantSpeciesId = params.plantId,
                        state = com.focusfarm.app.domain.GardenPlantState.WITHERED.name,
                        plantedAt = completedAt,
                        focusMinutes = params.durationMinutes,
                        seedsEarned = 0,
                        tagId = params.selectedTag.name,
                        growthStageReached = com.focusfarm.app.domain.GrowthStageConfig.DEFAULT.stageFor(params.durationMinutes).ordinal
                    )
                )
                id
            }
            return CompleteSessionResult(
                sessionId = failedSessionId,
                grantedSeeds = 0,
                baseSeeds = 0,
                wasBoosted = false,
                reminderRescheduled = false,
                grantedWaterDroplets = 0,
                grantedExperience = 0,
            )
        }

        progressRepository.ensureReady(nowMillis = completedAt, zoneId = clock.zoneId)
        val transactionResult = database.withTransaction {
            progressRepository.ensureUserProgressRowInTransaction(nowMillis = completedAt)
            userInventoryDao.ensureExists()
            val sessionId = sessionDao.insert(
                SessionEntity(
                    plantId = params.plantId,
                    plantName = params.plantName,
                    plantEmoji = params.plantEmoji,
                    durationMinutes = params.durationMinutes,
                    completedAt = completedAt,
                    selectedTag = params.selectedTag,
                    status = SessionStatus.COMPLETED,
                    failReason = null,
                )
            )
            val rewardGrant = progressRepository.grantSessionCompletionRewardInTransaction(
                sessionId = sessionId,
                durationMinutes = params.durationMinutes,
                nowMillis = completedAt,
            )
            database.gardenEntryDao().insert(
                com.focusfarm.app.data.local.GardenEntryEntity(
                    id = "ge-$sessionId",
                    sessionId = sessionId,
                    plantSpeciesId = params.plantId,
                    state = com.focusfarm.app.domain.GardenPlantState.BLOOMED.name,
                    plantedAt = completedAt,
                    focusMinutes = params.durationMinutes,
                    seedsEarned = rewardGrant.grantedSeeds,
                    tagId = params.selectedTag.name,
                    growthStageReached = com.focusfarm.app.domain.GrowthStageConfig.DEFAULT.stageFor(params.durationMinutes).ordinal
                )
            )
            var recoveryCompleted = false
            params.activeSessionId?.let { activeSessionId ->
                recoveryCompleted = recoverySessionDao.markCompleted(activeSessionId, completedAt) == 1
                check(activeSessionDao.delete(activeSessionId) == 1) {
                    "Active session was missing during completion: $activeSessionId"
                }
            }
            val baseWater = floor(params.durationMinutes * params.rewardMultiplier).toInt()
            val recoveryBonus = if (recoveryCompleted) params.durationMinutes / 2 else 0
            val grantedWater = baseWater + recoveryBonus
            check(userInventoryDao.grant(grantedWater, params.durationMinutes) == 1) {
                "User inventory was missing during session completion"
            }
            val recentSessions = sessionDao.getRecentSessionsSnapshot(limit = REMINDER_WINDOW_SIZE)
                .map { it.toDomain() }

            PersistedSessionResult(
                sessionId = sessionId,
                rewardGrant = rewardGrant,
                recentSessions = recentSessions,
                recoveryCompleted = recoveryCompleted,
                grantedWaterDroplets = grantedWater,
                grantedExperience = params.durationMinutes,
            )
        }

        runCatching {
            val dateKey = Instant.ofEpochMilli(completedAt).atZone(clock.zoneId).toLocalDate().toString()
            progressRepository.ensureMissionsForDate(dateKey, nowMillis = completedAt)
            progressRepository.updateMissionProgress(dateKey, nowMillis = completedAt)
        }

        runCatching {
            val freezeDates = progressRepository.observeFreezeDates(clock.zoneId).first()
            streakManager.recordDailyStreak(
                sessions = transactionResult.recentSessions,
                freezeDates = freezeDates,
                nowMillis = completedAt,
                zoneId = clock.zoneId,
            )
        }

        val reminderRescheduled = runCatching {
            FocusReminderScheduler.scheduleFromSessions(
                context = context,
                sessions = transactionResult.recentSessions,
            )
        }.isSuccess
        localCohortTracker.recordSessionCompleted(transactionResult.recoveryCompleted)

        return CompleteSessionResult(
            sessionId = transactionResult.sessionId,
            grantedSeeds = transactionResult.rewardGrant.grantedSeeds,
            baseSeeds = transactionResult.rewardGrant.baseSeeds,
            wasBoosted = transactionResult.rewardGrant.wasBoosted,
            reminderRescheduled = reminderRescheduled,
            grantedWaterDroplets = transactionResult.grantedWaterDroplets,
            grantedExperience = transactionResult.grantedExperience,
        )
    }

    private companion object {
        const val REMINDER_WINDOW_SIZE = 100
    }
}

private data class PersistedSessionResult(
    val sessionId: Long,
    val rewardGrant: SessionRewardGrant,
    val recentSessions: List<FocusSession>,
    val recoveryCompleted: Boolean,
    val grantedWaterDroplets: Int,
    val grantedExperience: Int,
)

private fun SessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    plantId = plantId,
    plantName = plantName,
    plantEmoji = plantEmoji,
    durationMinutes = durationMinutes,
    completedAt = completedAt,
    selectedTag = selectedTag,
    status = status,
    failReason = failReason,
)
