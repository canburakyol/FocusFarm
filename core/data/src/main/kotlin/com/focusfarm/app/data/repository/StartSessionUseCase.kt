package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.ActiveSessionEntity
import com.focusfarm.app.data.local.ActiveSessionStatus
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.time.SessionClock
import com.focusfarm.app.domain.FocusTag
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartSessionUseCase @Inject constructor(
    private val activeSessionDao: ActiveSessionDao,
    private val clock: SessionClock,
) {
    suspend operator fun invoke(
        plantId: String,
        targetMinutes: Int,
        selectedTag: FocusTag = FocusTag.OTHER,
        sessionType: FocusSessionType = FocusSessionType.COUNTDOWN,
        isStrict: Boolean = false,
        ambienceSound: AmbienceSound = AmbienceSound.NONE,
    ): ActiveSessionEntity {
        require(targetMinutes in 0..MAX_SESSION_MINUTES)
        val wall = clock.wallTimeMillis()
        val elapsed = clock.elapsedRealtimeMillis()
        val durationMs = Math.multiplyExact(targetMinutes.toLong(), 60_000L)
        val session = ActiveSessionEntity(
            id = UUID.randomUUID().toString(),
            plantId = plantId,
            targetMinutes = targetMinutes,
            startedAtEpochMs = wall,
            targetEndEpochMs = Math.addExact(wall, durationMs),
            startedAtElapsedMs = elapsed,
            targetEndElapsedMs = Math.addExact(elapsed, durationMs),
            bootMarker = clock.bootMarker(),
            status = ActiveSessionStatus.RUNNING,
            remainingSecondsAtInterruption = null,
            lastHeartbeatEpochMs = wall,
            createdAtEpochMs = wall,
            updatedAtEpochMs = wall,
            selectedTag = selectedTag,
            sessionType = sessionType,
            isStrict = isStrict,
            ambienceSound = ambienceSound,
            pausedRemainingSeconds = null,
            accumulatedElapsedSeconds = null,
        )
        activeSessionDao.upsert(session)
        return session
    }

    private companion object { const val MAX_SESSION_MINUTES = 24 * 60 }
}
