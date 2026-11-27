package com.focusfarm.app.data.repository

import com.focusfarm.app.time.Clock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDailyFocusTrendUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val clock: Clock
) {
    operator fun invoke(filter: TimeFilter): Flow<List<DailyTrendPoint>> {
        val (start, end) = filter.getBounds(clock)
        return statsRepository.getDailyFocusTrend(start, end)
    }
}
