package com.focusfarm.app.ai

import com.focusfarm.app.domain.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZoneId
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

data class SessionRecommendation(
    val suggestedTime: String,
    val suggestedDuration: Int,
    val confidence: Float,
    val reason: String,
)

@Singleton
class SessionRecommender @Inject constructor() {

    fun recommendBestTime(
        sessions: List<FocusSession>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<SessionRecommendation> = flow {
        if (sessions.isEmpty()) {
            emit(
                SessionRecommendation(
                    suggestedTime = "10:00",
                    suggestedDuration = 25,
                    confidence = 0.5f,
                    reason = "Genel olarak sabah saatleri en verimli odak zamanidir.",
                )
            )
            return@flow
        }

        val hourCounts = mutableMapOf<Int, Int>()
        val hourSuccess = mutableMapOf<Int, Int>()

        sessions.forEach { session ->
            val hour = Instant.ofEpochMilli(session.completedAt)
                .atZone(zoneId)
                .hour

            hourCounts[hour] = hourCounts.getOrDefault(hour, 0) + 1
            hourSuccess[hour] = hourSuccess.getOrDefault(hour, 0) + session.durationMinutes
        }

        val bestHour = hourSuccess.maxByOrNull { it.value }?.key ?: 10
        val totalSessions = sessions.size
        val bestHourSessions = hourCounts[bestHour] ?: 0
        val confidence = (bestHourSessions.toFloat() / totalSessions).coerceIn(0.3f, 1.0f)

        val avgDuration = if (bestHourSessions > 0) {
            (hourSuccess[bestHour] ?: 0) / bestHourSessions
        } else {
            25
        }

        val suggestedDuration = when {
            avgDuration >= 45 -> 45
            avgDuration >= 30 -> 30
            avgDuration >= 20 -> 25
            else -> 15
        }

        val timeLabel = String.format("%02d:00", bestHour)
        val reason = when {
            bestHour in 6..9 -> "Sabah saatleri senin icin en verimli. $bestHourSessions oturum tamamladin."
            bestHour in 10..12 -> "Ogle oncesi odaklanma performansi yuksek. $bestHourSessions oturum tamamladin."
            bestHour in 13..17 -> "Ogle sonrasi saatleri senin golden saatlerin. $bestHourSessions oturum tamamladin."
            bestHour in 18..21 -> "Aksam saatleri senin icin en verimli. $bestHourSessions oturum tamamladin."
            else -> "Bu saat diliminde en cok basarili oturumun var. $bestHourSessions oturum tamamladin."
        }

        emit(
            SessionRecommendation(
                suggestedTime = timeLabel,
                suggestedDuration = suggestedDuration,
                confidence = confidence,
                reason = reason,
            )
        )
    }

    fun recommendDuration(
        sessions: List<FocusSession>,
        currentStreak: Int,
    ): Int {
        if (sessions.isEmpty()) return 25

        val recentSessions = sessions.take(10)
        val avgDuration = recentSessions.map { it.durationMinutes }.average().toInt()

        return when {
            currentStreak >= 14 -> avgDuration.coerceIn(25, 60)
            currentStreak >= 7 -> avgDuration.coerceIn(20, 45)
            currentStreak >= 3 -> avgDuration.coerceIn(15, 30)
            else -> 15
        }
    }

    fun getBestDayOfWeek(
        sessions: List<FocusSession>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DayOfWeek {
        if (sessions.isEmpty()) return DayOfWeek.MONDAY

        val dayCounts = mutableMapOf<DayOfWeek, Int>()
        sessions.forEach { session ->
            val day = Instant.ofEpochMilli(session.completedAt)
                .atZone(zoneId)
                .dayOfWeek
            dayCounts[day] = dayCounts.getOrDefault(day, 0) + 1
        }

        return dayCounts.maxByOrNull { it.value }?.key ?: DayOfWeek.MONDAY
    }
}
