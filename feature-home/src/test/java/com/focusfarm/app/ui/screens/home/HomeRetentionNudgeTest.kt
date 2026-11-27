package com.focusfarm.app.ui.screens.home

import com.focusfarm.core.ui.R
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

        assertThat(nudge.titleRes).isEqualTo(R.string.nudge_streak_title)
        assertThat(nudge.detailArg).isEqualTo(4)
        assertThat(nudge.tone).isEqualTo(HomeRetentionNudge.Tone.URGENT)
    }

    @Test
    fun partialDailyGoalShowsTheRemainingFocusTarget() {
        val nudge = HomeRetentionNudge.from(
            todayFocusMinutes = 35,
            dailyGoalMinutes = 60,
            currentStreak = 2,
        )

        assertThat(nudge.titleRes).isEqualTo(R.string.nudge_progress_title)
        assertThat(nudge.detailArg).isEqualTo(25)
        assertThat(nudge.tone).isEqualTo(HomeRetentionNudge.Tone.PROGRESS)
    }

    @Test
    fun reachedGoalCelebratesWithoutAFormatArgument() {
        val nudge = HomeRetentionNudge.from(todayFocusMinutes = 60, dailyGoalMinutes = 60, currentStreak = 0)

        assertThat(nudge.titleRes).isEqualTo(R.string.nudge_celebrate_title)
        assertThat(nudge.detailArg).isNull()
        assertThat(nudge.tone).isEqualTo(HomeRetentionNudge.Tone.CELEBRATE)
    }
}
