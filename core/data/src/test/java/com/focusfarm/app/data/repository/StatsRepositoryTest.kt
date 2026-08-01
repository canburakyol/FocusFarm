package com.focusfarm.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.GardenEntryEntity
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.data.local.WalletTransactionEntity
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.SessionStatus
import com.focusfarm.app.time.Clock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class StatsRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: StatsRepositoryImpl
    private val clock = TestClock(LocalDate.of(2026, 7, 11))

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

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

        repository = StatsRepositoryImpl(
            sessionDao = database.sessionDao(),
            gardenEntryDao = database.gardenEntryDao(),
            walletDao = database.walletDao(),
            progressDao = database.progressDao(),
            clock = clock
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getStatsAggregatesCorrectly() = runTest {
        val now = clock.nowMillis()
        
        // Insert sessions
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 25,
                completedAt = now,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )
        database.sessionDao().insert(
            SessionEntity(
                plantId = "cactus",
                plantName = "Cactus",
                plantEmoji = "🌵",
                durationMinutes = 15,
                completedAt = now - 5000,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.WORK
            )
        )
        database.sessionDao().insert(
            SessionEntity(
                plantId = "cactus",
                plantName = "Cactus",
                plantEmoji = "🌵",
                durationMinutes = 10,
                completedAt = now - 10000,
                status = SessionStatus.FAILED,
                selectedTag = FocusTag.WORK
            )
        )

        // Insert garden entry
        database.gardenEntryDao().insert(
            GardenEntryEntity(
                id = "1",
                sessionId = 1L,
                plantSpeciesId = "sprout",
                state = "BLOOMED",
                plantedAt = now,
                focusMinutes = 25,
                seedsEarned = 25,
                tagId = "STUDY",
                growthStageReached = 3
            )
        )

        // Insert wallet transactions
        database.walletDao().insertTransaction(
            WalletTransactionEntity(
                id = "tx1",
                userId = "user_0",
                type = "REWARD",
                amount = 25,
                sourceType = "FOCUS",
                sourceId = "1",
                createdAt = now
            )
        )

        val totalMinutes = repository.getTotalMinutesInRange(now - 20000, now + 20000).first()
        val successCount = repository.getSuccessfulSessionCountInRange(now - 20000, now + 20000).first()
        val failedCount = repository.getFailedSessionCountInRange(now - 20000, now + 20000).first()
        val longest = repository.getLongestSessionMinutesInRange(now - 20000, now + 20000).first()
        val grown = repository.getGrownPlantsCountInRange(now - 20000, now + 20000).first()
        val seeds = repository.getSeedsEarnedInRange(now - 20000, now + 20000).first()

        assertThat(totalMinutes).isEqualTo(40) // 25 + 15
        assertThat(successCount).isEqualTo(2)
        assertThat(failedCount).isEqualTo(1)
        assertThat(longest).isEqualTo(25)
        assertThat(grown).isEqualTo(1)
        assertThat(seeds).isEqualTo(25)
    }

    @Test
    fun getDailyFocusTrendGroupsAndCalculatesCorrectly() = runTest {
        val now = clock.nowMillis()
        val oneDayMillis = 86_400_000L

        // Today
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 25,
                completedAt = now,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )

        // Yesterday
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 45,
                completedAt = now - oneDayMillis,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.WORK
            )
        )

        val trend = repository.getDailyFocusTrend(now - 2 * oneDayMillis, now + oneDayMillis).first()
        assertThat(trend).hasSize(2)
        
        val yesterdayTrend = trend.find { it.totalMinutes == 45 }
        val todayTrend = trend.find { it.totalMinutes == 25 }

        assertThat(yesterdayTrend).isNotNull()
        assertThat(todayTrend).isNotNull()
        assertThat(yesterdayTrend?.completedSessions).isEqualTo(1)
        assertThat(todayTrend?.completedSessions).isEqualTo(1)
    }

    @Test
    fun getTagDistributionCalculatesCorrectly() = runTest {
        val now = clock.nowMillis()

        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 25,
                completedAt = now,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )
        database.sessionDao().insert(
            SessionEntity(
                plantId = "cactus",
                plantName = "Cactus",
                plantEmoji = "🌵",
                durationMinutes = 15,
                completedAt = now,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.WORK
            )
        )

        val dist = repository.getTagDistribution(now - 10000, now + 10000).first()
        assertThat(dist).hasSize(2)

        val studyDist = dist.find { it.selectedTag == FocusTag.STUDY }
        val workDist = dist.find { it.selectedTag == FocusTag.WORK }

        assertThat(studyDist?.totalMinutes).isEqualTo(25)
        assertThat(workDist?.totalMinutes).isEqualTo(15)
    }

    @Test
    fun getStreaksCalculatesConsecutiveAndBestCorrectly() = runTest {
        val now = clock.nowMillis()
        val oneDayMillis = 86_400_000L

        // Today active (completed 30 minutes, goal is 25)
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 30,
                completedAt = now,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )

        // Yesterday active
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 25,
                completedAt = now - oneDayMillis,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )

        // Two days ago inactive (only 10 mins, does not meet daily goal of 25)
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 10,
                completedAt = now - 2 * oneDayMillis,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )

        // Three and four days ago active
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 25,
                completedAt = now - 3 * oneDayMillis,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )
        database.sessionDao().insert(
            SessionEntity(
                plantId = "sprout",
                plantName = "Sprout",
                plantEmoji = "🌱",
                durationMinutes = 25,
                completedAt = now - 4 * oneDayMillis,
                status = SessionStatus.COMPLETED,
                selectedTag = FocusTag.STUDY
            )
        )

        val (currentStreak, bestStreak) = repository.getStreaks(25, emptySet()).first()

        assertThat(currentStreak).isEqualTo(2) // Today and Yesterday
        assertThat(bestStreak).isEqualTo(2)    // max consecutive is 2
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
}
