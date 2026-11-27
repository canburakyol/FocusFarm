package com.focusfarm.app.ui.screens.home

import android.content.Context
import android.content.SharedPreferences
import com.focusfarm.app.data.local.ActiveSessionDao
import com.focusfarm.app.data.local.ActiveSessionEntity
import com.focusfarm.app.data.local.ActiveSessionStatus
import com.focusfarm.app.data.local.RecoverySessionDao
import com.focusfarm.app.data.local.RecoverySessionEntity
import com.focusfarm.app.data.local.RecoverySessionStatus
import com.focusfarm.app.data.local.UnlockedSeedDao
import com.focusfarm.app.data.repository.UnlockedPlantsRepository
import com.focusfarm.app.data.local.UnlockedSeedEntity
import com.focusfarm.app.data.local.GardenEntryDao
import com.focusfarm.app.data.local.GardenEntryEntity
import com.focusfarm.app.data.local.GardenEntryWithSpecies
import com.focusfarm.app.data.repository.OnboardingPreferences
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.QuestRerollState
import com.focusfarm.app.data.repository.RewardGrantRepository
import com.focusfarm.app.data.repository.SessionRewardGrant
import com.focusfarm.app.data.repository.StatsRepository
import com.focusfarm.app.data.repository.DailyTrendPoint
import com.focusfarm.app.data.repository.TagDuration
import com.focusfarm.app.data.repository.SessionRecoveryManager
import com.focusfarm.app.data.repository.StartSessionUseCase
import com.focusfarm.app.data.repository.ClaimRewardedAdRewardUseCase
import com.focusfarm.app.data.billing.FakeBillingRepository
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.monetization.ConsentManager
import com.focusfarm.app.monetization.ConsentState
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.app.domain.DailyQuestProgress
import com.focusfarm.app.domain.DailyQuestType
import com.focusfarm.app.domain.Mission
import com.focusfarm.app.domain.MissionProgress
import com.focusfarm.app.time.Clock
import com.focusfarm.app.time.SessionClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeContext: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeContext = FakeContext(FakeSharedPreferences())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startEmitsNavigationEffectWithCurrentSelection() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(HomeIntent.SelectDuration(25))
        val effect = async { viewModel.effect.collectOne() }

        viewModel.onIntent(HomeIntent.StartTodaySession)
        advanceUntilIdle()

        val open = effect.await() as HomeEffect.OpenSession
        assertThat(open.plantId).isEqualTo("sprout")
        assertThat(open.minutes).isEqualTo(25)
        assertThat(open.sessionId).isNotEmpty()
        assertThat(viewModel.uiState.value.isStartingSession).isFalse()
    }

    private fun createViewModel(): HomeViewModel {
        val dao = HomeFakeActiveSessionDao()
        val sessionClock = HomeFakeSessionClock()
        val clock = HomeFakeClock()
        val progressRepository = HomeFakeProgressRepository()
        val rewardGrantRepository = HomeFakeRewardGrantRepository()
        return HomeViewModel(
            startSession = StartSessionUseCase(dao, sessionClock),
            recoveryManager = SessionRecoveryManager(dao, HomeFakeRecoverySessionDao(), sessionClock),
            unlockedPlantsRepository = UnlockedPlantsRepository(HomeFakeUnlockedSeedDao(), HomeFakeInventoryDao()),
            progressRepository = progressRepository,
            statsRepository = HomeFakeStatsRepository(),
            gardenEntryDao = HomeFakeGardenEntryDao(),
            onboardingPreferences = OnboardingPreferences(fakeContext),
            clock = clock,
            billingRepository = FakeBillingRepository(),
            rewardedAdManager = RewardedAdManager(HomeFakeTelemetry(), HomeFakeConsentManager()),
            rewardGrantRepository = rewardGrantRepository,
            claimRewardedAdReward = ClaimRewardedAdRewardUseCase(rewardGrantRepository, progressRepository),
        )
    }
}

private class FakeContext(private val prefs: SharedPreferences) : android.content.ContextWrapper(null) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return prefs
    }
    override fun getApplicationContext(): Context {
        return this
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = map[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { temp[key] = values; return this }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { temp[key] = value; return this }
        override fun remove(key: String): SharedPreferences.Editor { temp.remove(key); return this }
        override fun clear(): SharedPreferences.Editor { temp.clear(); return this }
        override fun commit(): Boolean { map.putAll(temp); return true }
        override fun apply() { map.putAll(temp) }
    }
}

private class HomeFakeRecoverySessionDao : RecoverySessionDao {
    override suspend fun insert(entity: RecoverySessionEntity) = 1L
    override suspend fun get(id: String): RecoverySessionEntity? = null
    override suspend fun updateStatus(id: String, status: RecoverySessionStatus) = 0
    override suspend fun markCompleted(id: String, completedAt: Long) = 0
    override suspend fun countOfferedSince(sinceEpochMs: Long) = 0
    override suspend fun completedTimestampsSince(sinceEpochMs: Long) = emptyList<Long>()
    override fun observeCompletedTimestamps() = flowOf(emptyList<Long>())
    override suspend fun countCompletedBetween(start: Long, end: Long) = 0
}

private class HomeFakeUnlockedSeedDao : UnlockedSeedDao {
    override suspend fun isUnlocked(seedId: String) = false
    override suspend fun unlock(entity: UnlockedSeedEntity) = 1L
    override fun observeUnlockedSeedIds() = flowOf(emptyList<String>())
}

private class HomeFakeProgressRepository : ProgressRepository {
    override fun observeSeeds(): Flow<Int> = flowOf(0)
    override fun observeFreezeTokens(): Flow<Int> = flowOf(0)
    override fun observeNextSessionDoubleSeedBoostActive(): Flow<Boolean> = flowOf(false)
    override fun observeClaimedQuestTypes(dateKey: String): Flow<Set<DailyQuestType>> = flowOf(emptySet())
    override fun observeQuestRerollState(dateKey: String): Flow<QuestRerollState?> = flowOf(null)
    override fun observeFreezeDates(zoneId: ZoneId): Flow<Set<LocalDate>> = flowOf(emptySet())
    override fun observeClaimedWeeklyRewardKeys(): Flow<Set<String>> = flowOf(emptySet())
    override fun observeClaimedCollectionRewardPlantIds(): Flow<Set<String>> = flowOf(emptySet())
    override suspend fun ensureInitializedAndGrantWeeklyFreeze(nowMillis: Long, zoneId: ZoneId) {}
    override suspend fun claimQuest(dateKey: String, quest: DailyQuestProgress, nowMillis: Long): Boolean = false
    override suspend fun useFreezeForDate(dateKey: String, nowMillis: Long): Boolean = false
    override suspend fun claimWeeklyReward(weekKey: String, rewardSeeds: Int, nowMillis: Long): Boolean = false
    override suspend fun claimCollectionReward(plantId: String, rewardSeeds: Int, nowMillis: Long): Boolean = false
    override suspend fun saveQuestRerollState(dateKey: String, questType: DailyQuestType, target: Int, rewardSeeds: Int, nowMillis: Long): Boolean = false
    override suspend fun armNextSessionDoubleSeedBoost(nowMillis: Long): Boolean = false
    override suspend fun grantSessionCompletionReward(sessionId: Long, durationMinutes: Int, nowMillis: Long): SessionRewardGrant {
        return SessionRewardGrant(0, 0, false)
    }
    override suspend fun ensureReady(nowMillis: Long, zoneId: ZoneId) {}
    override suspend fun ensureUserProgressRowInTransaction(nowMillis: Long, zoneId: ZoneId) {}
    override suspend fun grantSessionCompletionRewardInTransaction(sessionId: Long, durationMinutes: Int, nowMillis: Long): SessionRewardGrant {
        return SessionRewardGrant(0, 0, false)
    }
    override fun observeDailyGoalMinutes(): Flow<Int> = flowOf(60)
    override suspend fun updateDailyGoalMinutes(minutes: Int) {}
    override fun observeActiveMissions(nowMillis: Long): Flow<List<Mission>> = flowOf(emptyList())
    override fun observeMissionProgress(missionIds: List<String>): Flow<List<MissionProgress>> = flowOf(emptyList())
    override suspend fun ensureMissionsForDate(dateKey: String, nowMillis: Long) {}
    override suspend fun rerollMissionsForDate(dateKey: String, nowMillis: Long) {}
    override suspend fun updateMissionProgress(dateKey: String, nowMillis: Long) {}
    override suspend fun claimMissionReward(missionId: String, nowMillis: Long): Int = 0
}

private class HomeFakeRewardGrantRepository : RewardGrantRepository {
    override suspend fun hasClaimedRewardToday(placement: String, dateKey: String): Boolean = false
    override fun observeHasClaimedRewardToday(placement: String, dateKey: String): Flow<Boolean> = flowOf(false)
    override suspend fun grantReward(
        transactionId: String,
        placement: String,
        sourceId: String,
        amount: Int,
        nowMillis: Long,
    ): Boolean = true
}

private class HomeFakeTelemetry : AppTelemetry {
    override fun logEvent(name: String, params: Map<String, String>) {}
    override fun recordNonFatal(tag: String, message: String, throwable: Throwable?) {}
}

private class HomeFakeConsentManager : ConsentManager {
    override val state = MutableStateFlow(ConsentState(canRequestAds = false))
    override fun refreshConsent(activity: android.app.Activity?) {}
    override fun showPrivacyOptions(activity: android.app.Activity, onDismissed: (String?) -> Unit) {
        onDismissed(null)
    }
}

private class HomeFakeStatsRepository : StatsRepository {
    override fun getTotalMinutesInRange(start: Long, end: Long): Flow<Int> = flowOf(0)
    override fun getSuccessfulSessionCountInRange(start: Long, end: Long): Flow<Int> = flowOf(0)
    override fun getFailedSessionCountInRange(start: Long, end: Long): Flow<Int> = flowOf(0)
    override fun getLongestSessionMinutesInRange(start: Long, end: Long): Flow<Int> = flowOf(0)
    override fun getDailyFocusTrend(start: Long, end: Long): Flow<List<DailyTrendPoint>> = flowOf(emptyList())
    override fun getTagDistribution(start: Long, end: Long): Flow<List<TagDuration>> = flowOf(emptyList())
    override fun getGrownPlantsCountInRange(start: Long, end: Long): Flow<Int> = flowOf(0)
    override fun getSeedsEarnedInRange(start: Long, end: Long): Flow<Int> = flowOf(0)
    override fun getStreaks(dailyGoalMinutes: Int, freezeDates: Set<LocalDate>): Flow<Pair<Int, Int>> = flowOf(Pair(0, 0))
}

private class HomeFakeGardenEntryDao : GardenEntryDao {
    override suspend fun insert(entry: GardenEntryEntity) {}
    override fun observeAllWithSpecies(): Flow<List<GardenEntryWithSpecies>> = flowOf(emptyList())
    override fun observeSinceWithSpecies(sinceEpochMs: Long): Flow<List<GardenEntryWithSpecies>> = flowOf(emptyList())
    override fun observeSinceExcludingWitheredWithSpecies(sinceEpochMs: Long): Flow<List<GardenEntryWithSpecies>> = flowOf(emptyList())
    override fun observeAllExcludingWitheredWithSpecies(): Flow<List<GardenEntryWithSpecies>> = flowOf(emptyList())
    override fun observeBloomedCount(): Flow<Int> = flowOf(0)
    override fun observeTotalCount(): Flow<Int> = flowOf(0)
    override fun getGrownPlantsCountInRange(start: Long, end: Long): Flow<Int> = flowOf(0)
}

private class HomeFakeClock : Clock {
    override val zoneId: ZoneId = ZoneId.of("UTC")
    override fun nowMillis(): Long = 10_000L
    override fun currentDateFlow(): Flow<LocalDate> = flowOf(LocalDate.of(2026, 3, 15))
}

private suspend fun Flow<HomeEffect>.collectOne(): HomeEffect = first()

private class HomeFakeSessionClock : SessionClock {
    override fun wallTimeMillis() = 10_000L
    override fun elapsedRealtimeMillis() = 5_000L
    override fun bootMarker() = 1_000L
}

private class HomeFakeActiveSessionDao : ActiveSessionDao {
    private var active: ActiveSessionEntity? = null

    override fun observeActive() = flowOf(active)
    override suspend fun getActive() = active
    override suspend fun upsert(session: ActiveSessionEntity) {
        active = session
    }
    override suspend fun updateRecoveryState(
        sessionId: String,
        status: ActiveSessionStatus,
        remaining: Int?,
        updatedAt: Long,
    ) = 0
    override suspend fun delete(sessionId: String): Int {
        active = null
        return 1
    }
}

private class HomeFakeInventoryDao : com.focusfarm.app.data.local.InventoryDao {
    override suspend fun insertItem(item: com.focusfarm.app.data.local.InventoryItemEntity) = 1L
    override suspend fun hasItem(itemType: String, itemId: String) = false
    override fun observeAllItems(userId: String) = flowOf(emptyList<com.focusfarm.app.data.local.InventoryItemEntity>())
    override fun observeItemsByType(itemType: String, userId: String) =
        flowOf(emptyList<com.focusfarm.app.data.local.InventoryItemEntity>())
    override suspend fun getAllItemsSnapshot(userId: String) = emptyList<com.focusfarm.app.data.local.InventoryItemEntity>()
}
