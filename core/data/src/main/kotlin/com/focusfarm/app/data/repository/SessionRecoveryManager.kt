package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.ActiveSessionStatus
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.RecoverySessionEntity
import com.focusfarm.app.data.local.RecoverySessionStatus
import com.focusfarm.app.time.SessionClock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RecoveryDecision {
    data object None : RecoveryDecision
    data class Resume(val sessionId: String, val remainingSeconds: Int) : RecoveryDecision
    data class ConfirmCompletion(val sessionId: String) : RecoveryDecision
    data class OfferRecovery(val sessionId: String, val suggestedMinutes: Int) : RecoveryDecision
    data class FinalizationRequired(val sessionId: String) : RecoveryDecision
}

@Singleton
class SessionRecoveryManager @Inject constructor(
    private val activeSessionDao: ActiveSessionDao,
    private val recoverySessionDao: RecoverySessionDao,
    private val clock: SessionClock,
) {
    suspend fun reconcile(): RecoveryDecision {
        val session = activeSessionDao.getActive() ?: return RecoveryDecision.None

        when (session.status) {
            ActiveSessionStatus.FINALIZING,
            ActiveSessionStatus.COMPLETION_PENDING,
            -> return RecoveryDecision.FinalizationRequired(session.id)

            ActiveSessionStatus.RECOVERY_REQUIRED -> return offerRecovery(session)

            ActiveSessionStatus.PAUSED -> {
                val remaining = session.pausedRemainingSeconds ?: (session.targetMinutes * 60)
                return RecoveryDecision.Resume(session.id, remaining)
            }

            ActiveSessionStatus.RUNNING -> Unit
        }

        val nowElapsed = clock.elapsedRealtimeMillis().coerceAtLeast(0L)
        val nowWall = clock.wallTimeMillis().coerceAtLeast(0L)
        val elapsedTimelineValid =
            session.startedAtElapsedMs >= 0L &&
                session.targetEndElapsedMs >= session.startedAtElapsedMs &&
                nowElapsed >= session.startedAtElapsedMs
        val sameBoot = elapsedTimelineValid &&
            absoluteDifference(session.bootMarker, clock.bootMarker()) <= BOOT_MARKER_TOLERANCE_MS

        if (sameBoot) {
            val remainingMs = positiveDifference(session.targetEndElapsedMs, nowElapsed)
            return if (remainingMs > 0L) {
                RecoveryDecision.Resume(session.id, millisToSecondsCeil(remainingMs))
            } else {
                RecoveryDecision.ConfirmCompletion(session.id)
            }
        }

        val wallTimelineValid =
            session.startedAtEpochMs >= 0L &&
                session.targetEndEpochMs >= session.startedAtEpochMs &&
                nowWall >= session.startedAtEpochMs
        val remainingWallMs = if (wallTimelineValid) {
            positiveDifference(session.targetEndEpochMs, nowWall)
        } else {
            0L
        }

        return if (wallTimelineValid && remainingWallMs > 0L) {
            RecoveryDecision.Resume(session.id, millisToSecondsCeil(remainingWallMs))
        } else {
            activeSessionDao.updateRecoveryState(
                sessionId = session.id,
                status = ActiveSessionStatus.RECOVERY_REQUIRED,
                remaining = null,
                updatedAt = nowWall,
            )
            offerRecovery(session)
        }
    }

    private suspend fun offerRecovery(session: com.focusfarm.app.data.local.ActiveSessionEntity): RecoveryDecision.OfferRecovery {
        val suggestedMinutes = recoveryMinutes(session.targetMinutes)
        recoverySessionDao.insert(
            RecoverySessionEntity(
                activeSessionId = session.id,
                plantId = session.plantId,
                suggestedMinutes = suggestedMinutes,
                status = RecoverySessionStatus.OFFERED,
                createdAtEpochMs = clock.wallTimeMillis(),
                completedAtEpochMs = null,
            ),
        )
        return RecoveryDecision.OfferRecovery(session.id, suggestedMinutes)
    }

    private fun recoveryMinutes(targetMinutes: Int): Int =
        (targetMinutes / 3).coerceIn(MIN_RECOVERY_MINUTES, MAX_RECOVERY_MINUTES)

    private fun millisToSecondsCeil(value: Long): Int {
        val seconds = value / 1_000L + if (value % 1_000L == 0L) 0L else 1L
        return seconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun positiveDifference(end: Long, start: Long): Long =
        if (end <= start) 0L else (end - start).coerceAtLeast(0L)

    private fun absoluteDifference(first: Long, second: Long): Long = when {
        (first xor second) < 0L -> Long.MAX_VALUE
        first >= second -> positiveDifference(first, second)
        else -> positiveDifference(second, first)
    }

    private companion object {
        const val BOOT_MARKER_TOLERANCE_MS = 2 * 60_000L
        const val MIN_RECOVERY_MINUTES = 3
        const val MAX_RECOVERY_MINUTES = 8
    }
}
