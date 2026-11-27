package com.focusfarm.app.data.repository

import com.focusfarm.app.domain.FocusTag
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class DailyTrendPoint(
    val localDay: Long, // epoch day number (e.g. LocalDate.toEpochDay())
    val totalMinutes: Int,
    val completedSessions: Int
)

data class TagDuration(
    val selectedTag: FocusTag,
    val totalMinutes: Int
)

interface StatsRepository {
    fun getTotalMinutesInRange(start: Long, end: Long): Flow<Int>
    fun getSuccessfulSessionCountInRange(start: Long, end: Long): Flow<Int>
    fun getFailedSessionCountInRange(start: Long, end: Long): Flow<Int>
    fun getLongestSessionMinutesInRange(start: Long, end: Long): Flow<Int>
    fun getDailyFocusTrend(start: Long, end: Long): Flow<List<DailyTrendPoint>>
    fun getTagDistribution(start: Long, end: Long): Flow<List<TagDuration>>
    fun getGrownPlantsCountInRange(start: Long, end: Long): Flow<Int>
    fun getSeedsEarnedInRange(start: Long, end: Long): Flow<Int>
    fun getStreaks(dailyGoalMinutes: Int, freezeDates: Set<LocalDate>): Flow<Pair<Int, Int>> // Pair(currentStreak, bestStreak)
}
