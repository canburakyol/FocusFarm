package com.focusfarm.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.notifications.FocusReminderScheduler
import com.focusfarm.app.time.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class CompleteSessionParams(
    val plantId: String,
    val plantName: String,
    val plantEmoji: String,
    val durationMinutes: Int,
    val completedAt: Long? = null,
)

data class CompleteSessionResult(
    val sessionId: Long,
    val grantedSeeds: Int,
    val baseSeeds: Int,
    val wasBoosted: Boolean,
    val reminderRescheduled: Boolean,
)

@Singleton
class CompleteSessionUseCase @Inject constructor(
    private val database: AppDatabase,
    private val sessionDao: SessionDao,
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(params: CompleteSessionParams): CompleteSessionResult {
        val completedAt = params.completedAt ?: clock.nowMillis()
        progressRepository.ensureReady(nowMillis = completedAt, zoneId = clock.zoneId)
        val transactionResult = database.withTransaction {
            progressRepository.ensureUserProgressRowInTransaction(nowMillis = completedAt)
            val sessionId = sessionDao.insert(
                SessionEntity(
                    plantId = params.plantId,
                    plantName = params.plantName,
                    plantEmoji = params.plantEmoji,
                    durationMinutes = params.durationMinutes,
                    completedAt = completedAt,
                )
            )
            val rewardGrant = progressRepository.grantSessionCompletionRewardInTransaction(
                durationMinutes = params.durationMinutes,
                nowMillis = completedAt,
            )
            val recentSessions = sessionDao.getRecentSessionsSnapshot(limit = REMINDER_WINDOW_SIZE)
                .map { it.toDomain() }

            PersistedSessionResult(
                sessionId = sessionId,
                rewardGrant = rewardGrant,
                recentSessions = recentSessions,
            )
        }

        val reminderRescheduled = runCatching {
            FocusReminderScheduler.scheduleFromSessions(
                context = context,
                sessions = transactionResult.recentSessions,
            )
        }.isSuccess

        return CompleteSessionResult(
            sessionId = transactionResult.sessionId,
            grantedSeeds = transactionResult.rewardGrant.grantedSeeds,
            baseSeeds = transactionResult.rewardGrant.baseSeeds,
            wasBoosted = transactionResult.rewardGrant.wasBoosted,
            reminderRescheduled = reminderRescheduled,
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
)

private fun SessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    plantId = plantId,
    plantName = plantName,
    plantEmoji = plantEmoji,
    durationMinutes = durationMinutes,
    completedAt = completedAt,
)
