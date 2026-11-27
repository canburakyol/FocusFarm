package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import java.time.ZoneOffset
import org.junit.Test

class FocusProgressCompletedOnlyTest {
    private val zone = ZoneOffset.UTC
    private val noon = 1_795_780_800_000L // 2026-11-27 12:00 UTC

    private fun session(minutes: Int, status: SessionStatus, atMillis: Long = noon) = FocusSession(
        plantId = "sprout",
        plantName = "Sprout",
        plantEmoji = "",
        durationMinutes = minutes,
        completedAt = atMillis,
        status = status,
    )

    @Test
    fun witheredSessionsDoNotCountAsTodaysFocus() {
        val sessions = listOf(
            session(5, SessionStatus.COMPLETED),
            session(25, SessionStatus.FAILED),
            session(25, SessionStatus.CANCELLED),
        )

        assertThat(FocusProgressEngine.todayMinutes(sessions, noon + 60_000, zone)).isEqualTo(5)
    }

    @Test
    fun failingSessionsCannotFillTheDailyGoalOrStartAStreak() {
        val sessions = listOf(session(25, SessionStatus.FAILED), session(25, SessionStatus.FAILED))

        assertThat(FocusProgressEngine.currentStreakDays(sessions, dailyGoalMinutes = 10, nowMillis = noon, zoneId = zone))
            .isEqualTo(0)
    }

    @Test
    fun completedFocusStillReachesTheGoal() {
        val sessions = listOf(session(10, SessionStatus.COMPLETED), session(25, SessionStatus.FAILED))

        assertThat(FocusProgressEngine.currentStreakDays(sessions, dailyGoalMinutes = 10, nowMillis = noon, zoneId = zone))
            .isEqualTo(1)
    }
}
