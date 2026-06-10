package com.focusfarm.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.time.Clock
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
class SessionPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeSessionUseCasePersistsSessionRewardAndConsumesBoost() = runTest {
        val clock = TestClock(LocalDate.of(2026, 3, 15))
        val progressRepository = ProgressRepository(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
        )
        val useCase = CompleteSessionUseCase(
            database = database,
            sessionDao = database.sessionDao(),
            progressRepository = progressRepository,
            clock = clock,
            context = context,
        )

        progressRepository.armNextSessionDoubleSeedBoost()

        val result = useCase(
            CompleteSessionParams(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "*",
                durationMinutes = 25,
            )
        )

        val storedSessions = database.sessionDao().getRecentSessionsSnapshot(limit = 10)
        val progress = database.progressDao().getUserProgress()

        assertThat(storedSessions).hasSize(1)
        assertThat(result.grantedSeeds).isEqualTo(result.baseSeeds * 2)
        assertThat(progress?.seeds).isEqualTo(result.grantedSeeds)
        assertThat(progress?.nextSessionDoubleSeedBoostActive).isFalse()
        assertThat(result.reminderRescheduled).isTrue()
    }

    @Test
    fun failedCompletionTransactionRollsBackSessionRewardAndBoostConsumption() = runTest {
        val clock = TestClock(LocalDate.of(2026, 3, 15))
        val progressRepository = ProgressRepository(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
        )
        progressRepository.armNextSessionDoubleSeedBoost()

        runCatching {
            database.withTransaction {
                progressRepository.ensureUserProgressRowInTransaction(nowMillis = clock.nowMillis())
                database.sessionDao().insert(
                    SessionEntity(
                        plantId = "sprout",
                        plantName = "Sprout",
                        plantEmoji = "*",
                        durationMinutes = 30,
                        completedAt = clock.nowMillis(),
                    )
                )
                progressRepository.grantSessionCompletionRewardInTransaction(
                    durationMinutes = 30,
                    nowMillis = clock.nowMillis(),
                )
                error("boom")
            }
        }

        val storedSessions = database.sessionDao().getRecentSessionsSnapshot(limit = 10)
        val progress = database.progressDao().getUserProgress()

        assertThat(storedSessions).isEmpty()
        assertThat(progress?.seeds).isEqualTo(0)
        assertThat(progress?.nextSessionDoubleSeedBoostActive).isTrue()
    }

    @Test
    fun legacyRewardPrefsMigrateIntoDatabaseOnce() = runTest {
        val clock = TestClock(LocalDate.of(2026, 3, 15))
        val prefs = context.getSharedPreferences("ad_reward_state", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("next_session_double_seed_active", true)
            .putString("quest_reroll_date_key", "2026-03-15")
            .putString("quest_reroll_type", DailyQuestType.FOCUS_MINUTES.name)
            .putInt("quest_reroll_target", 45)
            .putInt("quest_reroll_reward_seeds", 18)
            .apply()

        val progressRepository = ProgressRepository(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
        )

        progressRepository.ensureInitializedAndGrantWeeklyFreeze()

        val rerollState = progressRepository.observeQuestRerollState("2026-03-15").first()
        val boostActive = progressRepository.observeNextSessionDoubleSeedBoostActive().first()

        assertThat(boostActive).isTrue()
        assertThat(rerollState?.questType).isEqualTo(DailyQuestType.FOCUS_MINUTES)
        assertThat(rerollState?.target).isEqualTo(45)
        assertThat(rerollState?.rewardSeeds).isEqualTo(18)
        assertThat(prefs.contains("quest_reroll_date_key")).isFalse()
        assertThat(prefs.contains("next_session_double_seed_active")).isFalse()
    }
}

private class TestClock(
    initialDate: LocalDate,
    override val zoneId: ZoneId = ZoneId.of("Europe/Istanbul"),
) : Clock {
    private val currentDate = MutableStateFlow(initialDate)

    override fun nowMillis(): Long = currentDate.value
        .atTime(12, 0)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()

    override fun currentDateFlow(): Flow<LocalDate> = currentDate
}
