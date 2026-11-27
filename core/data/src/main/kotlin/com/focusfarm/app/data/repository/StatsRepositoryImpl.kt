package com.focusfarm.app.data.repository

import com.focusfarm.app.data.local.SessionDao
import com.focusfarm.app.data.local.GardenEntryDao
import com.focusfarm.app.data.local.WalletDao
import com.focusfarm.app.data.local.ProgressDao
import com.focusfarm.app.data.local.DailyTrendPointEntity
import com.focusfarm.app.data.local.TagDurationEntity
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val gardenEntryDao: GardenEntryDao,
    private val walletDao: WalletDao,
    private val progressDao: ProgressDao,
    private val clock: Clock
) : StatsRepository {

    private fun getTimezoneOffsetMillis(): Long {
        val zoneId = clock.zoneId
        val instant = Instant.ofEpochMilli(clock.nowMillis())
        return zoneId.rules.getOffset(instant).totalSeconds * 1000L
    }

    override fun getTotalMinutesInRange(start: Long, end: Long): Flow<Int> =
        sessionDao.getTotalMinutesInRange(start, end)

    override fun getSuccessfulSessionCountInRange(start: Long, end: Long): Flow<Int> =
        sessionDao.getSuccessfulSessionCountInRange(start, end)

    override fun getFailedSessionCountInRange(start: Long, end: Long): Flow<Int> =
        sessionDao.getFailedSessionCountInRange(start, end)

    override fun getLongestSessionMinutesInRange(start: Long, end: Long): Flow<Int> =
        sessionDao.getLongestSessionMinutesInRange(start, end)

    override fun getDailyFocusTrend(start: Long, end: Long): Flow<List<DailyTrendPoint>> {
        val offset = getTimezoneOffsetMillis()
        return sessionDao.getDailyFocusTrend(start, end, offset).map { list ->
            list.map {
                DailyTrendPoint(
                    localDay = it.localDay,
                    totalMinutes = it.totalMinutes,
                    completedSessions = it.completedSessions
                )
            }
        }
    }

    override fun getTagDistribution(start: Long, end: Long): Flow<List<TagDuration>> =
        sessionDao.getTagDistribution(start, end).map { list ->
            list.map {
                TagDuration(
                    selectedTag = it.selectedTag,
                    totalMinutes = it.totalMinutes
                )
            }
        }

    override fun getGrownPlantsCountInRange(start: Long, end: Long): Flow<Int> =
        gardenEntryDao.getGrownPlantsCountInRange(start, end)

    override fun getSeedsEarnedInRange(start: Long, end: Long): Flow<Int> =
        walletDao.getSeedsEarnedInRange(start, end)

    override fun getStreaks(dailyGoalMinutes: Int, freezeDates: Set<LocalDate>): Flow<Pair<Int, Int>> {
        val offset = getTimezoneOffsetMillis()
        return sessionDao.getAllCompletedDaysWithMinutes(offset).map { daysWithMinutes ->
            val activeDates = daysWithMinutes
                .filter { it.totalMinutes >= dailyGoalMinutes }
                .map { LocalDate.ofEpochDay(it.localDay) }
                .toSet() + freezeDates

            val today = clock.today()
            val (currentStreak, computedBest) = calculateStreaks(activeDates, today)
            
            val dbBest = progressDao.getBestStreak() ?: 0
            val bestStreak = maxOf(computedBest, dbBest)

            Pair(currentStreak, bestStreak)
        }
    }

    private fun calculateStreaks(activeDates: Set<LocalDate>, today: LocalDate): Pair<Int, Int> {
        if (activeDates.isEmpty()) return Pair(0, 0)

        // Current streak
        val hasToday = activeDates.contains(today)
        val hasYesterday = activeDates.contains(today.minusDays(1))
        val startDate = when {
            hasToday -> today
            hasYesterday -> today.minusDays(1)
            else -> null
        }
        var currentStreak = 0
        if (startDate != null) {
            var cursor: LocalDate = startDate
            while (activeDates.contains(cursor)) {
                currentStreak++
                cursor = cursor.minusDays(1)
            }
        }

        // Best streak (maximum consecutive days)
        var bestStreak = 0
        if (activeDates.isNotEmpty()) {
            val sortedDates = activeDates.sorted()
            var currentConsecutive = 0
            var prevDate: LocalDate? = null
            for (date in sortedDates) {
                if (prevDate == null) {
                    currentConsecutive = 1
                } else {
                    if (date == prevDate.plusDays(1)) {
                        currentConsecutive++
                    } else if (date != prevDate) {
                        bestStreak = maxOf(bestStreak, currentConsecutive)
                        currentConsecutive = 1
                    }
                }
                prevDate = date
            }
            bestStreak = maxOf(bestStreak, currentConsecutive)
        }

        return Pair(currentStreak, bestStreak)
    }
}
