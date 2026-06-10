package com.focusfarm.app.ui.screens.stats

import android.app.Activity
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.ReflectionRepository
import com.focusfarm.app.data.repository.ReferralResult
import com.focusfarm.app.data.repository.ReferralState
import com.focusfarm.app.time.Clock
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
    fun weeklyStateRollsIntoNewWeekWithoutRestart() = runTest {
        val clock = TestClock(initialDate = LocalDate.of(2026, 3, 15))
        val gardenRepository = GardenRepository(database.sessionDao())
        val progressRepository = ProgressRepository(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
        )
        val reflectionRepository = ReflectionRepository(database.reflectionDao())

        insertSession(
            completedAt = LocalDate.of(2026, 3, 15)
                .atTime(10, 0)
                .atZone(clock.zoneId)
                .toInstant()
                .toEpochMilli(),
            durationMinutes = 25,
        )

        val viewModel = StatsViewModel(
            repository = gardenRepository,
            progressRepository = progressRepository,
            reflectionRepository = reflectionRepository,
            clock = clock,
            settingsActions = FakeStatsSettingsActions(),
        )

        val weeklySessionsJob = backgroundScope.launch { viewModel.weeklySessions.collect { } }
        val weeklyChallengeJob = backgroundScope.launch { viewModel.weeklyChallenge.collect { } }

        advanceUntilIdle()

        assertThat(viewModel.weeklyChallenge.first { it.weekKey == "2026-03-09" }.weekKey)
            .isEqualTo("2026-03-09")
        assertThat(viewModel.weeklySessions.first { it.size == 1 }).hasSize(1)

        insertSession(
            completedAt = LocalDate.of(2026, 3, 16)
                .atTime(9, 0)
                .atZone(clock.zoneId)
                .toInstant()
                .toEpochMilli(),
            durationMinutes = 40,
        )
        clock.advanceTo(LocalDate.of(2026, 3, 16))
        advanceUntilIdle()

        assertThat(viewModel.weeklyChallenge.first { it.weekKey == "2026-03-16" }.weekKey)
            .isEqualTo("2026-03-16")
        val newWeekSessions = viewModel.weeklySessions.first { sessions ->
            sessions.size == 1 && sessions.single().durationMinutes == 40
        }
        assertThat(newWeekSessions).hasSize(1)
        assertThat(newWeekSessions.single().durationMinutes).isEqualTo(40)

        weeklySessionsJob.cancel()
        weeklyChallengeJob.cancel()
    }

    private suspend fun insertSession(
        completedAt: Long,
        durationMinutes: Int,
    ) {
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "*",
                durationMinutes = durationMinutes,
                completedAt = completedAt,
            )
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
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

    fun advanceTo(date: LocalDate) {
        currentDate.value = date
    }
}

private class FakeStatsSettingsActions : StatsSettingsActions {
    private val _referralState = MutableStateFlow(ReferralState())
    override val referralState: StateFlow<ReferralState> = _referralState

    override suspend fun verifyReferral(referralCode: String): ReferralResult =
        ReferralResult(success = true, message = "ok")

    override fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit) {
        onDismissed(null)
    }

    override suspend fun deleteAccount(): Boolean = true
}
