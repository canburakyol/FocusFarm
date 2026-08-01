package com.focusfarm.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.ActiveSessionEntity
import com.focusfarm.app.data.local.ActiveSessionStatus
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.time.Clock
import com.focusfarm.app.data.billing.FakeBillingRepository
import com.focusfarm.app.data.repository.ProgressRepositoryImpl
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
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
    private lateinit var billingRepository: FakeBillingRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dummyTelemetry = object : com.focusfarm.app.telemetry.AppTelemetry {
            override fun recordNonFatal(tag: String, message: String, throwable: Throwable?) {}
            override fun logEvent(name: String, params: Map<String, String>) {}
        }
        billingRepository = FakeBillingRepository()

        // Seed plant species for foreign key constraints
        val speciesEntities = com.focusfarm.app.domain.PlantSpeciesCatalog.ALL.map {
            com.focusfarm.app.data.local.PlantSpeciesEntity(
                id = it.id,
                slug = it.slug,
                displayName = it.displayName,
                description = it.description,
                rarity = it.rarity.name,
                growthStageCount = it.growthStageCount,
                minimumFocusMinutes = it.minimumFocusMinutes,
                seedCost = it.seedCost,
                unlockType = it.unlockType,
                isPremium = it.isPremium,
                isSeasonal = it.isSeasonal,
                assetKey = it.assetKey,
                sortOrder = it.sortOrder
            )
        }
        database.plantSpeciesDao().insertAll(speciesEntities)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeSessionUseCasePersistsSessionRewardAndConsumesBoost() = runTest {
        val clock = TestClock(LocalDate.of(2026, 3, 15))
        val progressRepository = ProgressRepositoryImpl(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
            billingRepository = billingRepository,
        )
        val streakManager = StreakManager(
            progressDao = database.progressDao(),
            recoverySessionDao = database.recoverySessionDao(),
            clock = clock,
        )
        val useCase = CompleteSessionUseCase(
            database = database,
            activeSessionDao = database.activeSessionDao(),
            recoverySessionDao = database.recoverySessionDao(),
            localCohortTracker = LocalCohortTracker(context, clock),
            sessionDao = database.sessionDao(),
            progressRepository = progressRepository,
            userInventoryDao = database.userInventoryDao(),
            streakManager = streakManager,
            clock = clock,
            context = context,
        )

        progressRepository.armNextSessionDoubleSeedBoost()
        database.activeSessionDao().upsert(
            ActiveSessionEntity(
                id = "active-1",
                plantId = "sprout",
                targetMinutes = 25,
                startedAtEpochMs = clock.nowMillis() - 1_500_000L,
                targetEndEpochMs = clock.nowMillis(),
                startedAtElapsedMs = 1_000L,
                targetEndElapsedMs = 1_501_000L,
                bootMarker = 0L,
                status = ActiveSessionStatus.FINALIZING,
                remainingSecondsAtInterruption = null,
                lastHeartbeatEpochMs = clock.nowMillis(),
                createdAtEpochMs = clock.nowMillis() - 1_500_000L,
                updatedAtEpochMs = clock.nowMillis(),
            )
        )

        val result = useCase(
            CompleteSessionParams(
                activeSessionId = "active-1",
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "*",
                durationMinutes = 25,
                selectedTag = FocusTag.STUDY,
            )
        )

        val storedSessions = database.sessionDao().getRecentSessionsSnapshot(limit = 10)
        val progress = database.progressDao().getUserProgress()

        assertThat(storedSessions).hasSize(1)
        assertThat(database.activeSessionDao().getActive()).isNull()
        assertThat(result.grantedSeeds).isEqualTo(166) // 25 base + 3 duration bonus + 50 first session + 5 streak = 83 * 2 (boost) = 166
        assertThat(progress?.seeds).isEqualTo(result.grantedSeeds)
        assertThat(progress?.nextSessionDoubleSeedBoostActive).isFalse()
        assertThat(result.reminderRescheduled).isTrue()
        assertThat(result.grantedWaterDroplets).isEqualTo(25)
        assertThat(database.userInventoryDao().get()?.totalExperience).isEqualTo(25)
        assertThat(storedSessions.single().selectedTag).isEqualTo(FocusTag.STUDY)

        // Verify Garden Entry is persisted
        val gardenEntries = database.gardenEntryDao().observeAllWithSpecies().first()
        assertThat(gardenEntries).hasSize(1)
        val entry = gardenEntries.first().entry
        assertThat(entry.plantSpeciesId).isEqualTo("sprout")
        assertThat(entry.state).isEqualTo(com.focusfarm.app.domain.GardenPlantState.BLOOMED.name)
        assertThat(entry.focusMinutes).isEqualTo(25)
        assertThat(entry.growthStageReached).isEqualTo(com.focusfarm.app.domain.GrowthStageConfig.DEFAULT.stageFor(25).ordinal)
    }

    @Test
    fun failedCompletionTransactionRollsBackSessionRewardAndBoostConsumption() = runTest {
        val clock = TestClock(LocalDate.of(2026, 3, 15))
        val progressRepository = ProgressRepositoryImpl(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
            billingRepository = billingRepository,
        )
        progressRepository.armNextSessionDoubleSeedBoost()

        runCatching {
            database.withTransaction {
                progressRepository.ensureUserProgressRowInTransaction(nowMillis = clock.nowMillis())
                val sessionId = database.sessionDao().insert(
                    SessionEntity(
                        plantId = "sprout",
                        plantName = "Sprout",
                        plantEmoji = "*",
                        durationMinutes = 30,
                        completedAt = clock.nowMillis(),
                    )
                )
                progressRepository.grantSessionCompletionRewardInTransaction(
                    sessionId = sessionId,
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

        val progressRepository = ProgressRepositoryImpl(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
            billingRepository = billingRepository,
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

    @Test
    fun completeSessionUseCaseHandlesFailedSessionAndWiltedPlant() = runTest {
        val clock = TestClock(LocalDate.of(2026, 3, 15))
        val progressRepository = ProgressRepositoryImpl(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
            billingRepository = billingRepository,
        )
        val streakManager = StreakManager(
            progressDao = database.progressDao(),
            recoverySessionDao = database.recoverySessionDao(),
            clock = clock,
        )
        val useCase = CompleteSessionUseCase(
            database = database,
            activeSessionDao = database.activeSessionDao(),
            recoverySessionDao = database.recoverySessionDao(),
            localCohortTracker = LocalCohortTracker(context, clock),
            sessionDao = database.sessionDao(),
            progressRepository = progressRepository,
            userInventoryDao = database.userInventoryDao(),
            streakManager = streakManager,
            clock = clock,
            context = context,
        )

        database.activeSessionDao().upsert(
            ActiveSessionEntity(
                id = "active-failed",
                plantId = "sunflower",
                targetMinutes = 25,
                startedAtEpochMs = clock.nowMillis() - 600_000L,
                targetEndEpochMs = clock.nowMillis() + 900_000L,
                startedAtElapsedMs = 1_000L,
                targetEndElapsedMs = 1_501_000L,
                bootMarker = 0L,
                status = ActiveSessionStatus.RUNNING,
                remainingSecondsAtInterruption = null,
                lastHeartbeatEpochMs = clock.nowMillis(),
                createdAtEpochMs = clock.nowMillis() - 600_000L,
                updatedAtEpochMs = clock.nowMillis(),
            )
        )

        val result = useCase(
            CompleteSessionParams(
                activeSessionId = "active-failed",
                plantId = "sunflower",
                plantName = "Sunflower",
                plantEmoji = "🌻",
                durationMinutes = 10,
                status = com.focusfarm.app.domain.SessionStatus.FAILED,
                failReason = "manual_abandon",
            )
        )

        val storedSessions = database.sessionDao().getRecentSessionsSnapshot(limit = 10)
        assertThat(storedSessions).hasSize(1)
        assertThat(storedSessions.single().status).isEqualTo(com.focusfarm.app.domain.SessionStatus.FAILED)
        assertThat(storedSessions.single().plantName).isEqualTo("Solmuş Bitki")
        assertThat(storedSessions.single().plantEmoji).isEqualTo("🥀")
        assertThat(storedSessions.single().failReason).isEqualTo("manual_abandon")
        assertThat(database.activeSessionDao().getActive()).isNull()

        // Rewards must be 0
        assertThat(result.grantedSeeds).isEqualTo(0)
        assertThat(result.grantedWaterDroplets).isEqualTo(0)

        // Verify Garden Entry is persisted with WITHERED state
        val gardenEntries = database.gardenEntryDao().observeAllWithSpecies().first()
        assertThat(gardenEntries).hasSize(1)
        val entry = gardenEntries.first().entry
        assertThat(entry.plantSpeciesId).isEqualTo("sunflower")
        assertThat(entry.state).isEqualTo(com.focusfarm.app.domain.GardenPlantState.WITHERED.name)
        assertThat(entry.focusMinutes).isEqualTo(10)
        assertThat(entry.growthStageReached).isEqualTo(com.focusfarm.app.domain.GrowthStageConfig.DEFAULT.stageFor(10).ordinal)
    }

    @Test
    fun completeSessionUseCaseFiltersFailedSessionsFromStats() = runTest {
        val clock = TestClock(LocalDate.of(2026, 3, 15))
        val progressRepository = ProgressRepositoryImpl(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
            billingRepository = billingRepository,
        )
        val streakManager = StreakManager(
            progressDao = database.progressDao(),
            recoverySessionDao = database.recoverySessionDao(),
            clock = clock,
        )
        val useCase = CompleteSessionUseCase(
            database = database,
            activeSessionDao = database.activeSessionDao(),
            recoverySessionDao = database.recoverySessionDao(),
            localCohortTracker = LocalCohortTracker(context, clock),
            sessionDao = database.sessionDao(),
            progressRepository = progressRepository,
            userInventoryDao = database.userInventoryDao(),
            streakManager = streakManager,
            clock = clock,
            context = context,
        )

        // Insert a completed session
        useCase(
            CompleteSessionParams(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 25,
                status = com.focusfarm.app.domain.SessionStatus.COMPLETED,
            )
        )

        // Insert a failed session
        useCase(
            CompleteSessionParams(
                plantId = "cactus",
                plantName = "Cactus",
                plantEmoji = "🌵",
                durationMinutes = 15,
                status = com.focusfarm.app.domain.SessionStatus.FAILED,
            )
        )

        val totalCount = database.sessionDao().getSessionCount().first()
        val totalMinutes = database.sessionDao().getTotalMinutes().first()

        assertThat(totalCount).isEqualTo(1) // only the completed one
        assertThat(totalMinutes).isEqualTo(25) // only the completed one
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
