package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.ProgressDao
import com.focusfarm.app.data.local.StreakHistoryEntity
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.domain.FocusProgressEngine
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class StreakSnapshot(
    val currentStreak: Int,
    val bestStreak: Int,
    val todaySessions: Int,
    val todayMinutes: Int,
    val isStreakAtRisk: Boolean,
    val freezeCandidateDate: LocalDate?,
)

@Singleton
class StreakManager @Inject constructor(
    private val progressDao: ProgressDao,
    private val recoverySessionDao: RecoverySessionDao,
    private val clock: Clock,
) {

    fun observeStreakHistory(limit: Int = 30): Flow<List<StreakHistoryEntity>> =
        progressDao.observeStreakHistory(limit)

    suspend fun getBestStreak(): Int =
        progressDao.getBestStreak() ?: 0

    suspend fun recordDailyStreak(
        sessions: List<FocusSession>,
        freezeDates: Set<LocalDate> = emptySet(),
        nowMillis: Long = clock.nowMillis(),
        zoneId: ZoneId = clock.zoneId,
    ) {
        val dailyGoalMinutes = progressDao.getUserProgress()?.dailyGoalMinutes ?: 10
        val protectedDates = freezeDates + recoveryAnchors(sessions, dailyGoalMinutes, freezeDates, nowMillis, zoneId)
        val streak = FocusProgressEngine.currentStreakDays(sessions, dailyGoalMinutes, protectedDates, nowMillis, zoneId)
        val todayMinutes = FocusProgressEngine.todayMinutes(sessions, nowMillis, zoneId)
        val todaySessions = sessions.count {
            FocusProgressEngine.isToday(it.completedAt, nowMillis, zoneId)
        }

        val dateKey = FocusProgressEngine.todayDateKey(nowMillis, zoneId)
        progressDao.insertStreakHistory(
            StreakHistoryEntity(
                dateKey = dateKey,
                streakCount = streak,
                sessionsCompleted = todaySessions,
                minutesFocused = todayMinutes,
                recordedAt = nowMillis,
            )
        )
    }

    suspend fun getStreakSnapshot(
        sessions: List<FocusSession>,
        freezeDates: Set<LocalDate> = emptySet(),
        nowMillis: Long = clock.nowMillis(),
        zoneId: ZoneId = clock.zoneId,
    ): StreakSnapshot {
        val dailyGoalMinutes = progressDao.getUserProgress()?.dailyGoalMinutes ?: 10
        val protectedDates = freezeDates + recoveryAnchors(sessions, dailyGoalMinutes, freezeDates, nowMillis, zoneId)
        val currentStreak = FocusProgressEngine.currentStreakDays(sessions, dailyGoalMinutes, protectedDates, nowMillis, zoneId)
        val todayMinutes = FocusProgressEngine.todayMinutes(sessions, nowMillis, zoneId)
        val todaySessions = sessions.count {
            FocusProgressEngine.isToday(it.completedAt, nowMillis, zoneId)
        }
        val freezeCandidate = FocusProgressEngine.freezeCandidateDate(sessions, dailyGoalMinutes, protectedDates, nowMillis, zoneId)
        val bestStreak = progressDao.getBestStreak() ?: 0

        return StreakSnapshot(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            todaySessions = todaySessions,
            todayMinutes = todayMinutes,
            isStreakAtRisk = freezeCandidate != null,
            freezeCandidateDate = freezeCandidate,
        )
    }

    private suspend fun recoveryAnchors(
        sessions: List<FocusSession>,
        dailyGoalMinutes: Int,
        freezeDates: Set<LocalDate>,
        nowMillis: Long,
        zoneId: ZoneId,
    ): Set<LocalDate> = FocusProgressEngine.recoveryProtectionDates(
        completedRecoveryTimestamps = recoverySessionDao.completedTimestampsSince(
            (nowMillis - RECOVERY_WINDOW_MS).coerceAtLeast(0L),
        ),
        sessions = sessions,
        dailyGoalMinutes = dailyGoalMinutes,
        existingProtectedDates = freezeDates,
        zoneId = zoneId,
    )

    private companion object { const val RECOVERY_WINDOW_MS = 24L * 60 * 60 * 1_000 }
}
