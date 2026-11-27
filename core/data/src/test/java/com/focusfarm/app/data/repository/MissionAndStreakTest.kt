package com.focusfarm.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.data.billing.FakeBillingRepository
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.GardenEntryEntity
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.SessionStatus
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.time.Clock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
class MissionAndStreakTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var clock: TestClock
    private lateinit var progressRepository: ProgressRepositoryImpl
    private lateinit var streakManager: StreakManager

    private class TestClock(var currentDate: LocalDate) : Clock {
        override val zoneId: ZoneId get() = ZoneId.systemDefault()
        override fun nowMillis(): Long = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        override fun currentDateFlow(): kotlinx.coroutines.flow.Flow<LocalDate> = kotlinx.coroutines.flow.flowOf(currentDate)
    }

    private val dummyTelemetry = object : AppTelemetry {
        override fun recordNonFatal(tag: String, message: String, throwable: Throwable?) {}
        override fun logEvent(name: String, params: Map<String, String>) {}
    }

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = TestClock(LocalDate.of(2026, 7, 10))

        val billingRepository = FakeBillingRepository()

        progressRepository = ProgressRepositoryImpl(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
            billingRepository = billingRepository,
        )

        streakManager = StreakManager(
            progressDao = database.progressDao(),
            recoverySessionDao = database.recoverySessionDao(),
            clock = clock,
        )

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

        progressRepository.ensureReady(clock.nowMillis())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dailyMissionsAreSeededCorrectlyForDate() = runTest {
        val dateKey = clock.currentDate.toString()
        progressRepository.ensureMissionsForDate(dateKey, clock.nowMillis())

        val activeMissions = progressRepository.observeActiveMissions(clock.nowMillis()).first()
        assertThat(activeMissions).hasSize(6)
        
        val completeSessions1 = activeMissions.first { it.id == "${dateKey}_COMPLETE_SESSIONS_1" }
        assertThat(completeSessions1.goalValue).isEqualTo(1)
        assertThat(completeSessions1.rewardSeeds).isEqualTo(10)
    }

    @Test
    fun sessionCompletionIncrementsMissionProgressAndCanBeClaimedOnce() = runTest {
        val dateKey = clock.currentDate.toString()
        progressRepository.ensureMissionsForDate(dateKey, clock.nowMillis())

        // Insert completed session in DB
        database.sessionDao().insert(
            SessionEntity(
                plantId = "carrot",
                plantName = "Havuç",
                plantEmoji = "🌱",
                durationMinutes = 25,
                completedAt = clock.nowMillis(),
                selectedTag = FocusTag.WORK,
                status = SessionStatus.COMPLETED
            )
        )

        database.gardenEntryDao().insert(
            GardenEntryEntity(
                id = "ge-1",
                sessionId = 1,
                plantSpeciesId = "carrot",
                state = "BLOOMED",
                plantedAt = clock.nowMillis(),
                focusMinutes = 25,
                seedsEarned = 10,
                tagId = "WORK",
                growthStageReached = 3
            )
        )

        progressRepository.updateMissionProgress(dateKey, clock.nowMillis())

        val activeMissions = progressRepository.observeActiveMissions(clock.nowMillis()).first()
        val missionIds = activeMissions.map { it.id }
        val progresses = progressRepository.observeMissionProgress(missionIds).first()
        val progressMap = progresses.associateBy { it.missionId }

        // Complete 1 session mission check
        val complete1Progress = progressMap["${dateKey}_COMPLETE_SESSIONS_1"]
        assertThat(complete1Progress).isNotNull()
        assertThat(complete1Progress!!.currentValue).isEqualTo(1)
        assertThat(complete1Progress.isCompleted).isTrue()
        assertThat(complete1Progress.isClaimed).isFalse()

        // Claim reward
        val rewardSeeds = progressRepository.claimMissionReward("${dateKey}_COMPLETE_SESSIONS_1", clock.nowMillis())
        assertThat(rewardSeeds).isEqualTo(10)

        // Seed balance check
        val seedBalance = progressRepository.observeSeeds().first()
        assertThat(seedBalance).isEqualTo(10)

        // Claim second time check (must fail/return 0)
        val doubleClaimReward = progressRepository.claimMissionReward("${dateKey}_COMPLETE_SESSIONS_1", clock.nowMillis())
        assertThat(doubleClaimReward).isEqualTo(0)

        val updatedProgresses = progressRepository.observeMissionProgress(missionIds).first()
        val updatedProgressMap = updatedProgresses.associateBy { it.missionId }
        assertThat(updatedProgressMap["${dateKey}_COMPLETE_SESSIONS_1"]!!.isClaimed).isTrue()
    }

    @Test
    fun dailyGoalMinutesControlsStreakCalculations() = runTest {
        // Complete session under 10 minutes goal (e.g. 5 minutes)
        database.sessionDao().insert(
            SessionEntity(
                plantId = "carrot",
                plantName = "Havuç",
                plantEmoji = "🌱",
                durationMinutes = 5,
                completedAt = clock.nowMillis(),
                selectedTag = FocusTag.WORK,
                status = SessionStatus.COMPLETED
            )
        )

        val sessions = listOf(
            FocusSession(
                id = 1,
                plantId = "carrot",
                plantName = "Havuç",
                plantEmoji = "🌱",
                durationMinutes = 5,
                completedAt = clock.nowMillis()
            )
        )

        // Streak with default goal = 10 minutes -> should be 0
        var snapshot = streakManager.getStreakSnapshot(sessions)
        assertThat(snapshot.currentStreak).isEqualTo(0)

        // Update daily goal target to 5 minutes
        progressRepository.updateDailyGoalMinutes(5)

        // Streak with goal = 5 minutes -> should be 1
        snapshot = streakManager.getStreakSnapshot(sessions)
        assertThat(snapshot.currentStreak).isEqualTo(1)
    }
}
