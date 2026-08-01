package com.focusfarm.app.data.repository

import androidx.room.withTransaction
import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.ActiveSessionStatus
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.RecoverySessionEntity
import com.focusfarm.app.data.local.RecoverySessionStatus
import com.focusfarm.app.time.SessionClock
import javax.inject.Inject

class RecoverySessionUseCase @Inject constructor(
    private val database: AppDatabase,
    private val activeSessionDao: ActiveSessionDao,
    private val recoverySessionDao: RecoverySessionDao,
    private val clock: SessionClock,
) {
    suspend operator fun invoke(activeSessionId: String, suggestedMinutes: Int): RecoverySessionEntity =
        database.withTransaction {
            val active = requireNotNull(activeSessionDao.getActive()?.takeIf { it.id == activeSessionId })
            val minutes = suggestedMinutes.coerceIn(3, 8)
            val nowWall = clock.wallTimeMillis()
            val nowElapsed = clock.elapsedRealtimeMillis()
            val durationMs = minutes * 60_000L
            activeSessionDao.upsert(
                active.copy(
                    targetMinutes = minutes,
                    startedAtEpochMs = nowWall,
                    targetEndEpochMs = nowWall + durationMs,
                    startedAtElapsedMs = nowElapsed,
                    targetEndElapsedMs = nowElapsed + durationMs,
                    bootMarker = clock.bootMarker(),
                    status = ActiveSessionStatus.RUNNING,
                    remainingSecondsAtInterruption = null,
                    updatedAtEpochMs = nowWall,
                ),
            )
            val recovery = RecoverySessionEntity(
                activeSessionId = active.id,
                plantId = active.plantId,
                suggestedMinutes = minutes,
                status = RecoverySessionStatus.RUNNING,
                createdAtEpochMs = nowWall,
                completedAtEpochMs = null,
            )
            val existing = recoverySessionDao.get(active.id)
            check(existing?.status != RecoverySessionStatus.COMPLETED) {
                "Completed recovery cannot be restarted: ${active.id}"
            }
            if (existing == null) {
                check(recoverySessionDao.insert(recovery) != -1L)
            } else {
                check(recoverySessionDao.updateStatus(active.id, RecoverySessionStatus.RUNNING) == 1)
            }
            requireNotNull(recoverySessionDao.get(active.id))
        }
}
