package com.focusfarm.app.domain

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

class FocusProgressEngineTest {
    private val zone: ZoneId = ZoneId.of("UTC")

    @Test
    fun `currentStreakDays counts consecutive days including today`() {
        val now = epoch("2026-02-22T12:00:00")
        val sessions = listOf(
            session("2026-02-22T10:00:00", 25),
            session("2026-02-21T11:00:00", 30),
            session("2026-02-20T09:00:00", 45),
        )

        val streak = FocusProgressEngine.currentStreakDays(
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        assertThat(streak).isEqualTo(3)
    }

    @Test
    fun `currentStreakDays continues from yesterday when today is empty`() {
        val now = epoch("2026-02-22T12:00:00")
        val sessions = listOf(
            session("2026-02-21T11:00:00", 30),
            session("2026-02-20T09:00:00", 45),
        )

        val streak = FocusProgressEngine.currentStreakDays(
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        assertThat(streak).isEqualTo(2)
    }

    @Test
    fun `currentStreakDays is zero when streak is broken`() {
        val now = epoch("2026-02-22T12:00:00")
        val sessions = listOf(
            session("2026-02-20T09:00:00", 45),
        )

        val streak = FocusProgressEngine.currentStreakDays(
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        assertThat(streak).isEqualTo(0)
    }

    @Test
    fun `currentStreakDays includes frozen days`() {
        val now = epoch("2026-02-22T12:00:00")
        val sessions = listOf(
            session("2026-02-20T09:00:00", 45),
        )
        val freezeDates = setOf(
            LocalDate.parse("2026-02-21"),
        )

        val streak = FocusProgressEngine.currentStreakDays(
            sessions = sessions,
            freezeDates = freezeDates,
            nowMillis = now,
            zoneId = zone,
        )

        assertThat(streak).isEqualTo(2)
    }

    @Test
    fun `dailyQuests maps progress from today's sessions only`() {
        val now = epoch("2026-02-22T12:00:00")
        val sessions = listOf(
            session("2026-02-22T07:00:00", 30),
            session("2026-02-22T08:00:00", 25),
            session("2026-02-21T09:00:00", 60),
        )

        val quests = FocusProgressEngine.dailyQuests(
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        val sessionQuest = quests.first { it.type == DailyQuestType.COMPLETE_SESSIONS }
        val minuteQuest = quests.first { it.type == DailyQuestType.FOCUS_MINUTES }
        val deepQuest = quests.first { it.type == DailyQuestType.DEEP_SESSION }

        assertThat(sessionQuest.progress).isEqualTo(2)
        assertThat(sessionQuest.isCompleted).isTrue()
        assertThat(minuteQuest.progress).isEqualTo(55)
        assertThat(minuteQuest.isCompleted).isFalse()
        assertThat(deepQuest.progress).isEqualTo(30)
        assertThat(deepQuest.isCompleted).isTrue()
    }

    @Test
    fun `weeklyChallenge uses current week sessions only`() {
        val now = epoch("2026-02-22T12:00:00") // Sunday
        val sessions = listOf(
            session("2026-02-17T10:00:00", 45), // this week
            session("2026-02-18T10:00:00", 30), // this week
            session("2026-02-21T10:00:00", 60), // this week
            session("2026-02-15T10:00:00", 120), // previous week
        )

        val challenge = FocusProgressEngine.weeklyChallenge(
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        assertThat(challenge.currentSessions).isEqualTo(3)
        assertThat(challenge.currentMinutes).isEqualTo(135)
        assertThat(challenge.isCompleted).isFalse()
    }

    @Test
    fun `freezeCandidateDate returns yesterday when one day gap exists`() {
        val now = epoch("2026-02-22T12:00:00")
        val sessions = listOf(
            session("2026-02-20T10:00:00", 25),
        )

        val candidate = FocusProgressEngine.freezeCandidateDate(
            sessions = sessions,
            nowMillis = now,
            zoneId = zone,
        )

        assertThat(candidate?.toString()).isEqualTo("2026-02-21")
    }

    private fun session(timestamp: String, minutes: Int): FocusSession = FocusSession(
        plantId = "sprout",
        plantName = "Sprout",
        plantEmoji = "🌱",
        durationMinutes = minutes,
        completedAt = epoch(timestamp),
    )

    private fun epoch(localDateTimeIso: String): Long =
        LocalDateTime.parse(localDateTimeIso)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
