package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import java.time.ZoneOffset
import org.junit.Test

class FocusWidgetSummaryTest {
    private val zone = ZoneOffset.UTC
    private val noon = 1_795_780_800_000L // 2026-11-27 12:00 UTC

    private fun session(minutes: Int, status: SessionStatus) = FocusSession(
        plantId = "sprout",
        plantName = "Sprout",
        plantEmoji = "",
        durationMinutes = minutes,
        completedAt = noon,
        status = status,
    )

    @Test
    fun countsOnlyCompletedFocusTowardTheGoal() {
        val summary = FocusWidgetSummary.from(
            sessions = listOf(session(15, SessionStatus.COMPLETED), session(25, SessionStatus.FAILED)),
            goalMinutes = 60,
            freezeDates = emptySet(),
            nowMillis = noon + 60_000,
            zoneId = zone,
        )

        assertThat(summary.todayMinutes).isEqualTo(15)
        assertThat(summary.progressPercent).isEqualTo(25)
        assertThat(summary.streakDays).isEqualTo(0)
    }

    @Test
    fun progressIsCappedAtFullAndSafeWithoutAGoal() {
        assertThat(FocusWidgetSummary(todayMinutes = 90, goalMinutes = 60, streakDays = 1).progressPercent).isEqualTo(100)
        assertThat(FocusWidgetSummary(todayMinutes = 10, goalMinutes = 0, streakDays = 0).progressPercent).isEqualTo(0)
    }
}
