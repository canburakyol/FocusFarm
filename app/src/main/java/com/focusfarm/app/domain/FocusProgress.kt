package com.focusfarm.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

enum class DailyQuestType {
    COMPLETE_SESSIONS,
    FOCUS_MINUTES,
    DEEP_SESSION,
}

data class DailyQuestProgress(
    val type: DailyQuestType,
    val target: Int,
    val progress: Int,
    val rewardSeeds: Int,
) {
    val isCompleted: Boolean
        get() = progress >= target

    val progressRatio: Float
        get() = if (target <= 0) 0f else (progress.toFloat() / target).coerceIn(0f, 1f)
}

data class WeeklyChallengeProgress(
    val weekKey: String,
    val seasonTitle: String,
    val seasonSubtitle: String,
    val targetMinutes: Int,
    val targetSessions: Int,
    val rewardSeeds: Int,
    val currentMinutes: Int,
    val currentSessions: Int,
) {
    val isCompleted: Boolean
        get() = currentMinutes >= targetMinutes && currentSessions >= targetSessions

    val minutesRatio: Float
        get() = if (targetMinutes <= 0) 0f else (currentMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)

    val sessionsRatio: Float
        get() = if (targetSessions <= 0) 0f else (currentSessions.toFloat() / targetSessions).coerceIn(0f, 1f)

    val overallRatio: Float
        get() = ((minutesRatio + sessionsRatio) / 2f).coerceIn(0f, 1f)
}

object FocusProgressEngine {
    private const val DAILY_TARGET_SESSIONS = 2
    private const val DAILY_TARGET_MINUTES = 60
    private const val DAILY_TARGET_DEEP_SESSION = 25
    private const val DAILY_REWARD_SESSIONS = 10
    private const val DAILY_REWARD_MINUTES = 15
    private const val DAILY_REWARD_DEEP = 20

    private const val WEEKLY_TARGET_MINUTES = 300
    private const val WEEKLY_TARGET_SESSIONS = 8

    private data class WeeklySeason(
        val title: String,
        val subtitle: String,
        val targetMinutes: Int,
        val targetSessions: Int,
        val rewardSeeds: Int,
    )

    fun currentStreakDays(
        sessions: List<FocusSession>,
        freezeDates: Set<LocalDate> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val activeDates = sessions
            .map { it.completedAt.toLocalDate(zoneId) }
            .plus(freezeDates)
            .toSet()
        if (activeDates.isEmpty()) return 0

        val today = nowMillis.toLocalDate(zoneId)
        val startDate = when {
            activeDates.contains(today) -> today
            activeDates.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        var cursor = startDate
        while (activeDates.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun todayMinutes(
        sessions: List<FocusSession>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val startOfDay = startOfDayMillis(nowMillis, zoneId)
        return sessions
            .asSequence()
            .filter { it.completedAt >= startOfDay }
            .sumOf { it.durationMinutes }
    }

    fun isToday(
        timestampMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val today = nowMillis.toLocalDate(zoneId)
        return timestampMillis.toLocalDate(zoneId) == today
    }

    fun dailyQuests(
        sessions: List<FocusSession>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<DailyQuestProgress> {
        val startOfDay = startOfDayMillis(nowMillis, zoneId)
        val todaySessions = sessions.filter { it.completedAt >= startOfDay }
        val totalMinutes = todaySessions.sumOf { it.durationMinutes }
        val longestSession = todaySessions.maxOfOrNull { it.durationMinutes } ?: 0

        return listOf(
            DailyQuestProgress(
                type = DailyQuestType.COMPLETE_SESSIONS,
                target = DAILY_TARGET_SESSIONS,
                progress = todaySessions.size,
                rewardSeeds = DAILY_REWARD_SESSIONS,
            ),
            DailyQuestProgress(
                type = DailyQuestType.FOCUS_MINUTES,
                target = DAILY_TARGET_MINUTES,
                progress = totalMinutes,
                rewardSeeds = DAILY_REWARD_MINUTES,
            ),
            DailyQuestProgress(
                type = DailyQuestType.DEEP_SESSION,
                target = DAILY_TARGET_DEEP_SESSION,
                progress = longestSession,
                rewardSeeds = DAILY_REWARD_DEEP,
            ),
        )
    }

    fun weeklyChallenge(
        sessions: List<FocusSession>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): WeeklyChallengeProgress {
        val startOfWeekDate = startOfWeekDate(nowMillis, zoneId)
        val startOfWeek = startOfWeekDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val weekSessions = sessions.filter { it.completedAt >= startOfWeek }
        val season = seasonForWeek(startOfWeekDate)
        return WeeklyChallengeProgress(
            weekKey = startOfWeekDate.toString(),
            seasonTitle = season.title,
            seasonSubtitle = season.subtitle,
            targetMinutes = season.targetMinutes,
            targetSessions = season.targetSessions,
            rewardSeeds = season.rewardSeeds,
            currentMinutes = weekSessions.sumOf { it.durationMinutes },
            currentSessions = weekSessions.size,
        )
    }

    fun freezeCandidateDate(
        sessions: List<FocusSession>,
        freezeDates: Set<LocalDate> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate? {
        val activeDates = sessions
            .map { it.completedAt.toLocalDate(zoneId) }
            .plus(freezeDates)
            .toSet()
        if (activeDates.isEmpty()) return null

        val today = nowMillis.toLocalDate(zoneId)
        val yesterday = today.minusDays(1)
        val dayBeforeYesterday = today.minusDays(2)

        val hasToday = activeDates.contains(today)
        val hasYesterday = activeDates.contains(yesterday)
        val hasDayBeforeYesterday = activeDates.contains(dayBeforeYesterday)

        return if (!hasToday && !hasYesterday && hasDayBeforeYesterday && !freezeDates.contains(yesterday)) {
            yesterday
        } else {
            null
        }
    }

    fun todayDateKey(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = nowMillis.toLocalDate(zoneId).toString()

    private fun startOfDayMillis(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val localDate = nowMillis.toLocalDate(zoneId)
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun startOfWeekMillis(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        return startOfWeekDate(nowMillis, zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun startOfWeekDate(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate {
        val today = nowMillis.toLocalDate(zoneId)
        val daysSinceMonday = (today.dayOfWeek.value - 1).toLong()
        return today.minusDays(daysSinceMonday)
    }

    private fun seasonForWeek(weekStartDate: LocalDate): WeeklySeason {
        val weekNumber = weekStartDate.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
        return when (weekNumber % 4) {
            0 -> WeeklySeason(
                title = "Derin Çalışma Haftası",
                subtitle = "Uzun odak bloklarıyla ritmini güçlendir.",
                targetMinutes = WEEKLY_TARGET_MINUTES,
                targetSessions = WEEKLY_TARGET_SESSIONS,
                rewardSeeds = 90,
            )

            1 -> WeeklySeason(
                title = "İstikrar Haftası",
                subtitle = "Her gün kısa ama net bir ilerleme.",
                targetMinutes = 220,
                targetSessions = 10,
                rewardSeeds = 80,
            )

            2 -> WeeklySeason(
                title = "Maraton Haftası",
                subtitle = "Toplam odak süreni zirveye taşı.",
                targetMinutes = 420,
                targetSessions = 8,
                rewardSeeds = 110,
            )

            else -> WeeklySeason(
                title = "Momentum Haftası",
                subtitle = "Kısa seanslarla ivmeni koru.",
                targetMinutes = 180,
                targetSessions = 14,
                rewardSeeds = 85,
            )
        }
    }
}

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

