package com.focusfarm.app.ui.screens.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeRetentionNudgeTest {
    @Test
    fun activeStreakWithoutTodayFocusPromptsAShortSessionToProtectIt() {
        val nudge = HomeRetentionNudge.from(
            todayFocusMinutes = 0,
            dailyGoalMinutes = 60,
            currentStreak = 4,
        )

        assertThat(nudge.title).isEqualTo("Serini koru")
        assertThat(nudge.detail).contains("4 günlük")
        assertThat(nudge.tone).isEqualTo(HomeRetentionNudge.Tone.URGENT)
    }

    @Test
    fun partialDailyGoalShowsTheRemainingFocusTarget() {
        val nudge = HomeRetentionNudge.from(
            todayFocusMinutes = 35,
            dailyGoalMinutes = 60,
            currentStreak = 2,
        )

        assertThat(nudge.title).isEqualTo("Ritmini sürdür")
        assertThat(nudge.detail).contains("25 dakika")
        assertThat(nudge.tone).isEqualTo(HomeRetentionNudge.Tone.PROGRESS)
    }
}
