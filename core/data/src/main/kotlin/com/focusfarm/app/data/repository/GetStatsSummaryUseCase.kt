package com.focusfarm.app.data.repository

import com.focusfarm.app.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class StatsSummary(
    val totalFocusDurationMinutes: Int,
    val successfulSessionCount: Int,
    val failedSessionCount: Int,
    val successRate: Float,
    val dailyAverageMinutes: Float,
    val longestSessionMinutes: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalPlantsGrown: Int,
    val totalSeedsEarned: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class GetStatsSummaryUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val progressRepository: ProgressRepository,
    private val clock: Clock
) {
    operator fun invoke(filter: TimeFilter): Flow<StatsSummary> {
        val (start, end) = filter.getBounds(clock)
        
        val dailyGoalMinutesFlow = progressRepository.observeDailyGoalMinutes()
        val freezeDatesFlow = progressRepository.observeFreezeDates(clock.zoneId)

        return combine(
            dailyGoalMinutesFlow,
            freezeDatesFlow
        ) { goal, freezes ->
            Pair(goal, freezes)
        }.flatMapLatest { (goal, freezes) ->
            val totalMinutesFlow = statsRepository.getTotalMinutesInRange(start, end)
            val successfulCountFlow = statsRepository.getSuccessfulSessionCountInRange(start, end)
            val failedCountFlow = statsRepository.getFailedSessionCountInRange(start, end)
            val longestSessionFlow = statsRepository.getLongestSessionMinutesInRange(start, end)
            val grownPlantsFlow = statsRepository.getGrownPlantsCountInRange(start, end)
            val seedsEarnedFlow = statsRepository.getSeedsEarnedInRange(start, end)
            val streaksFlow = statsRepository.getStreaks(goal, freezes)
            val completedDaysFlow = statsRepository.getDailyFocusTrend(0L, clock.nowMillis() + 60000L)

            combine(
                totalMinutesFlow,
                successfulCountFlow,
                failedCountFlow,
                longestSessionFlow,
                grownPlantsFlow,
                seedsEarnedFlow,
                streaksFlow,
                completedDaysFlow
            ) { args: Array<Any> ->
                val totalMin = args[0] as Int
                val successCount = args[1] as Int
                val failedCount = args[2] as Int
                val longestMin = args[3] as Int
                val grownCount = args[4] as Int
                val seedsEarned = args[5] as Int
                @Suppress("UNCHECKED_CAST")
                val streaks = args[6] as Pair<Int, Int>
                @Suppress("UNCHECKED_CAST")
                val completedDays = args[7] as List<DailyTrendPoint>

                val totalSessions = successCount + failedCount
                val successRate = if (totalSessions == 0) 0f else (successCount.toFloat() / totalSessions)
                
                val numberOfDays = if (filter == TimeFilter.ALL_TIME) {
                    val firstDayEpoch = completedDays.minOfOrNull { it.localDay }
                    if (firstDayEpoch != null) {
                        val firstDay = LocalDate.ofEpochDay(firstDayEpoch)
                        val diff = ChronoUnit.DAYS.between(firstDay, clock.today()) + 1
                        diff.coerceAtLeast(1).toInt()
                    } else {
                        1
                    }
                } else {
                    filter.getNumberOfDays(clock)
                }

                val dailyAverage = totalMin.toFloat() / numberOfDays

                StatsSummary(
                    totalFocusDurationMinutes = totalMin,
                    successfulSessionCount = successCount,
                    failedSessionCount = failedCount,
                    successRate = successRate,
                    dailyAverageMinutes = dailyAverage,
                    longestSessionMinutes = longestMin,
                    currentStreak = streaks.first,
                    bestStreak = streaks.second,
                    totalPlantsGrown = grownCount,
                    totalSeedsEarned = seedsEarned
                )
            }
        }
    }
}
