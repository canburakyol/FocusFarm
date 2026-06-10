package com.focusfarm.app.ui.screens.home

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focusfarm.app.data.billing.PremiumBillingState
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.data.local.SessionEntity
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.monetization.ConsentState
import com.focusfarm.app.monetization.MonetizationRepository
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.ConscryptMode.Mode.OFF
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@ConscryptMode(OFF)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
    fun dailyQuestClaimAndRerollResetWhenDateChangesWithoutRestart() = runTest {
        val clock = TestClock(initialDate = LocalDate.of(2026, 3, 15))
        val progressRepository = ProgressRepository(
            context = context,
            database = database,
            progressDao = database.progressDao(),
            clock = clock,
        )
        val gardenRepository = GardenRepository(database.sessionDao())
        val monetizationRepository = FakeMonetizationRepository(canShowAdsInitially = true)

        insertSession(
            completedAt = dateTimeMillis(clock, LocalDate.of(2026, 3, 15), 9),
            durationMinutes = 30,
        )
        insertSession(
            completedAt = dateTimeMillis(clock, LocalDate.of(2026, 3, 15), 11),
            durationMinutes = 30,
        )

        val viewModel = HomeViewModel(
            repository = gardenRepository,
            monetizationRepository = monetizationRepository,
            progressRepository = progressRepository,
            clock = clock,
            context = context,
        )

        val questsJob = backgroundScope.launch { viewModel.dailyQuests.collect { } }
        val rerollJob = backgroundScope.launch { viewModel.isQuestRerollUsedToday.collect { } }

        advanceUntilIdle()

        val focusQuest = viewModel.dailyQuests.value
            .first { it.quest.type == DailyQuestType.FOCUS_MINUTES }
            .quest
        progressRepository.claimQuest(
            dateKey = "2026-03-15",
            quest = focusQuest,
        )
        advanceUntilIdle()

        progressRepository.saveQuestRerollState(
            dateKey = "2026-03-15",
            questType = DailyQuestType.COMPLETE_SESSIONS,
            target = 1,
            rewardSeeds = 18,
        )
        advanceUntilIdle()

        val claimedQuests = viewModel.dailyQuests.first { quests ->
            quests.any { quest ->
                quest.quest.type == DailyQuestType.FOCUS_MINUTES && quest.isClaimed
            }
        }
        assertThat(
            claimedQuests.first { it.quest.type == DailyQuestType.FOCUS_MINUTES }.isClaimed
        ).isTrue()
        assertThat(viewModel.isQuestRerollUsedToday.first { it }).isTrue()

        clock.advanceTo(LocalDate.of(2026, 3, 16))
        advanceUntilIdle()

        assertThat(viewModel.isQuestRerollUsedToday.first { !it }).isFalse()
        val resetQuests = viewModel.dailyQuests.first { quests ->
            quests.isNotEmpty() && quests.none { it.isClaimed }
        }
        assertThat(resetQuests.any { it.isClaimed }).isFalse()

        questsJob.cancel()
        rerollJob.cancel()
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

    private fun dateTimeMillis(
        clock: TestClock,
        date: LocalDate,
        hourOfDay: Int,
    ): Long = date
        .atTime(hourOfDay, 0)
        .atZone(clock.zoneId)
        .toInstant()
        .toEpochMilli()
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

private class FakeMonetizationRepository(
    canShowAdsInitially: Boolean,
) : MonetizationRepository {
    private val billing = MutableStateFlow(PremiumBillingState(isPremiumUnlocked = false))
    private val adsAllowed = MutableStateFlow(canShowAdsInitially)

    override val billingState: StateFlow<PremiumBillingState> = billing
    override val canShowAds: StateFlow<Boolean> = adsAllowed

    override fun refreshConsent(activity: android.app.Activity?) = Unit

    override fun refreshBilling() = Unit

    override fun launchPurchase(activity: android.app.Activity, plan: PremiumPlan) = Unit

    override fun clearBillingMessage() = Unit

    override fun preloadRewarded(context: Context) = Unit

    override fun showRewarded(
        activity: android.app.Activity,
        placement: String,
        onRewardEarned: () -> Unit,
        onNotReady: () -> Unit,
    ) {
        if (canShowAds.value) {
            onRewardEarned()
        } else {
            onNotReady()
        }
    }
}
