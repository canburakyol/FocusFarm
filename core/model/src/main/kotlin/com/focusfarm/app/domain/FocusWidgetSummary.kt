package com.focusfarm.app.domain

import java.time.LocalDate
import java.time.ZoneId

/** What the home-screen widget shows. Only completed sessions count, as everywhere else. */
data class FocusWidgetSummary(val todayMinutes: Int, val goalMinutes: Int, val streakDays: Int) {
    val progressPercent: Int
        get() = if (goalMinutes <= 0) 0 else (todayMinutes * 100 / goalMinutes).coerceIn(0, 100)

    companion object {
        fun from(
            sessions: List<FocusSession>,
            goalMinutes: Int,
            freezeDates: Set<LocalDate>,
            nowMillis: Long,
            zoneId: ZoneId,
        ) = FocusWidgetSummary(
            todayMinutes = FocusProgressEngine.todayMinutes(sessions, nowMillis, zoneId),
            goalMinutes = goalMinutes,
            streakDays = FocusProgressEngine.currentStreakDays(sessions, goalMinutes, freezeDates, nowMillis, zoneId),
        )
    }
}
