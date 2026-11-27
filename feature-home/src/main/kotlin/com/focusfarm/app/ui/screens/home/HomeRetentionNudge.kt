package com.focusfarm.app.ui.screens.home

import androidx.annotation.StringRes
import com.focusfarm.core.ui.R

/** Contextual home-screen prompt based on today's goal and current streak. */
data class HomeRetentionNudge(
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int,
    /** Single format argument for [detailRes], or null when the detail has none. */
    val detailArg: Int?,
    val tone: Tone,
) {
    enum class Tone { WELCOME, PROGRESS, URGENT, CELEBRATE }

    companion object {
        fun from(
            todayFocusMinutes: Int,
            dailyGoalMinutes: Int,
            currentStreak: Int,
        ): HomeRetentionNudge {
            val today = todayFocusMinutes.coerceAtLeast(0)
            val goal = dailyGoalMinutes.coerceAtLeast(1)
            return when {
                today >= goal -> HomeRetentionNudge(
                    R.string.nudge_celebrate_title, R.string.nudge_celebrate_body, null, Tone.CELEBRATE,
                )
                today == 0 && currentStreak > 0 -> HomeRetentionNudge(
                    R.string.nudge_streak_title, R.string.nudge_streak_body, currentStreak, Tone.URGENT,
                )
                today > 0 -> HomeRetentionNudge(
                    R.string.nudge_progress_title, R.string.nudge_progress_body, goal - today, Tone.PROGRESS,
                )
                else -> HomeRetentionNudge(
                    R.string.nudge_welcome_title, R.string.nudge_welcome_body, null, Tone.WELCOME,
                )
            }
        }
    }
}
