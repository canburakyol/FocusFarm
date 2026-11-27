package com.focusfarm.app.data.repository

import com.focusfarm.app.time.Clock
import java.time.LocalDate

enum class TimeFilter {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME;

    fun getBounds(clock: Clock): Pair<Long, Long> {
        val zoneId = clock.zoneId
        val today = clock.today()
        val startLocalDate = when (this) {
            TODAY -> today
            LAST_7_DAYS -> today.minusDays(6)
            LAST_30_DAYS -> today.minusDays(29)
            THIS_MONTH -> today.withDayOfMonth(1)
            THIS_YEAR -> today.withDayOfYear(1)
            ALL_TIME -> return Pair(0L, clock.nowMillis() + 60_000L) // Add buffer
        }
        val startMillis = startLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return Pair(startMillis, endMillis)
    }

    fun getNumberOfDays(clock: Clock): Int {
        val today = clock.today()
        return when (this) {
            TODAY -> 1
            LAST_7_DAYS -> 7
            LAST_30_DAYS -> 30
            THIS_MONTH -> today.dayOfMonth
            THIS_YEAR -> today.dayOfYear
            ALL_TIME -> 1 // Handled dynamically in UseCase
        }
    }
}
