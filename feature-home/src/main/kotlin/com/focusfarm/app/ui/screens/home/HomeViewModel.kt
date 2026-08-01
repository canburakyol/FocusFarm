package com.focusfarm.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.local.UnlockedSeedDao
import com.focusfarm.app.data.local.GardenEntryDao
import com.focusfarm.app.data.repository.OnboardingPreferences
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.StatsRepository
import com.focusfarm.app.data.repository.TimeFilter
import com.focusfarm.app.data.repository.RecoveryDecision
import com.focusfarm.app.data.repository.SessionRecoveryManager
import com.focusfarm.app.data.repository.StartSessionUseCase
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.FocusTag
import com.focusfarm.app.domain.FocusSessionType
import com.focusfarm.app.domain.AmbienceSound
import com.focusfarm.app.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.focusfarm.app.data.billing.BillingRepository
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.repository.RewardGrantRepository
import com.focusfarm.app.data.repository.ClaimRewardedAdRewardUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val QUICK_DURATIONS = listOf(15, 25, 45, 60, 90)

data class TodayGrownPlant(
    val emoji: String,
    val name: String,
    val durationMinutes: Int,
)

data class HomeMissionItem(
    val mission: com.focusfarm.app.domain.Mission,
    val progress: com.focusfarm.app.domain.MissionProgress?,
)

data class HomeUiState(
    val selectedMinutes: Int = 25,
    val quickDurations: List<Int> = QUICK_DURATIONS,
    val selectedPlantId: String = PlantCatalog.FREE.first().id,
    val isStartingSession: Boolean = false,
    val selectedTag: FocusTag = FocusTag.WORK,
    val availableTags: List<FocusTag> = FocusTag.entries,
    val availablePlants: List<Plant> = emptyList(),
    val sessionType: FocusSessionType = FocusSessionType.COUNTDOWN,
    val isStrict: Boolean = false,
    val ambienceSound: AmbienceSound = AmbienceSound.NONE,

    // Dashboard Stats
    val seedBalance: Int = 0,
    val dailyGoalMinutes: Int = 60,
    val todayFocusMinutes: Int = 0,
    val currentStreak: Int = 0,
    val retentionNudge: HomeRetentionNudge = HomeRetentionNudge.from(0, 60, 0),
    val todayGrownPlants: List<TodayGrownPlant> = emptyList(),
    val activeMissions: List<HomeMissionItem> = emptyList(),
    val isPremium: Boolean = false,
    val dailySeedBonusClaimed: Boolean = false,
)

sealed interface HomeIntent {
    data class SelectDuration(val minutes: Int) : HomeIntent
    data class SelectTag(val tag: FocusTag) : HomeIntent
    data class SelectPlant(val plantId: String) : HomeIntent
    data class SelectSessionType(val type: FocusSessionType) : HomeIntent
    data class SetStrictFocus(val strict: Boolean) : HomeIntent
    data class SelectAmbience(val sound: AmbienceSound) : HomeIntent
    data object StartTodaySession : HomeIntent
    data class ClaimMission(val missionId: String) : HomeIntent
    data class ClaimDailySeedBonus(val activity: android.app.Activity?) : HomeIntent
}

sealed interface HomeEffect {
    data class OpenSession(
        val sessionId: String,
        val plantId: String,
        val minutes: Int,
    ) : HomeEffect
    data class ResumeSession(val sessionId: String) : HomeEffect
    data class ShowRecovery(val sessionId: String, val suggestedMinutes: Int) : HomeEffect
    data class FinalizeSession(val sessionId: String) : HomeEffect
    data class MissionClaimed(val rewardSeeds: Int) : HomeEffect
    data class ShowError(val message: String) : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startSession: StartSessionUseCase,
    private val recoveryManager: SessionRecoveryManager,
    private val unlockedSeedDao: UnlockedSeedDao,
    private val progressRepository: ProgressRepository,
    private val statsRepository: StatsRepository,
    private val gardenEntryDao: GardenEntryDao,
    private val onboardingPreferences: OnboardingPreferences,
    private val clock: Clock,
    private val billingRepository: BillingRepository,
    private val rewardedAdManager: RewardedAdManager,
    private val rewardGrantRepository: RewardGrantRepository,
    private val claimRewardedAdReward: ClaimRewardedAdRewardUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val effects = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        // Set default duration chosen during onboarding
        val onboardingDuration = onboardingPreferences.getDefaultDuration()
        _uiState.update { it.copy(selectedMinutes = onboardingDuration) }

        viewModelScope.launch {
            when (val decision = recoveryManager.reconcile()) {
                is RecoveryDecision.Resume -> effects.send(HomeEffect.ResumeSession(decision.sessionId))
                is RecoveryDecision.OfferRecovery -> effects.send(
                    HomeEffect.ShowRecovery(decision.sessionId, decision.suggestedMinutes),
                )
                is RecoveryDecision.FinalizationRequired -> effects.send(
                    HomeEffect.FinalizeSession(decision.sessionId),
                )
                RecoveryDecision.None,
                is RecoveryDecision.ConfirmCompletion,
                -> Unit
            }
        }

        viewModelScope.launch {
            unlockedSeedDao.observeUnlockedSeedIds().collectLatest { unlockedIds ->
                val available = PlantCatalog.ALL.filter { !it.isPremium || unlockedIds.contains(it.id) }
                _uiState.update { it.copy(
                    availablePlants = available,
                    selectedPlantId = if (available.any { p -> p.id == it.selectedPlantId }) it.selectedPlantId else available.first().id
                ) }
            }
        }

        // Observe and sync dashboard data
        viewModelScope.launch {
            val (start, end) = TimeFilter.TODAY.getBounds(clock)
            val todayDateKey = clock.today().toString()

            // Prepare daily missions
            try {
                progressRepository.ensureMissionsForDate(todayDateKey, clock.nowMillis())
                progressRepository.updateMissionProgress(todayDateKey, clock.nowMillis())
            } catch (e: Exception) {
                // Suppress background setup errors
            }

            val seedFlow = progressRepository.observeSeeds()
            val goalFlow = progressRepository.observeDailyGoalMinutes()
            val freezesFlow = progressRepository.observeFreezeDates(clock.zoneId)
            val todayMinutesFlow = statsRepository.getTotalMinutesInRange(start, end)

            val streakFlow = combine(goalFlow, freezesFlow) { goal, freezes ->
                Pair(goal, freezes)
            }.flatMapLatest { (goal, freezes) ->
                statsRepository.getStreaks(goal, freezes)
            }

            val grownPlantsFlow = gardenEntryDao.observeSinceWithSpecies(start).map { list ->
                list.filter { it.entry.state == "BLOOMED" }.map {
                    TodayGrownPlant(
                        emoji = PlantCatalog.getById(it.entry.plantSpeciesId)?.emoji ?: "🌱",
                        name = it.species.displayName,
                        durationMinutes = it.entry.focusMinutes
                    )
                }
            }

            val activeMissionsFlow = progressRepository.observeActiveMissions(clock.nowMillis()).flatMapLatest { missions ->
                val ids = missions.map { it.id }
                progressRepository.observeMissionProgress(ids).map { progresses ->
                    val progressMap = progresses.associateBy { it.missionId }
                    missions.map { mission ->
                        HomeMissionItem(
                            mission = mission,
                            progress = progressMap[mission.id]
                        )
                    }
                }
            }

            val isPremiumFlow = billingRepository.entitlement.map { it.isPremiumUnlocked }
            val dailySeedBonusClaimedFlow = rewardGrantRepository.observeHasClaimedRewardToday("daily_seed_bonus", todayDateKey)

            combine(
                seedFlow,
                goalFlow,
                todayMinutesFlow,
                streakFlow,
                grownPlantsFlow,
                activeMissionsFlow,
                isPremiumFlow,
                dailySeedBonusClaimedFlow
            ) { args: Array<Any> ->
                val seeds = args[0] as Int
                val goal = args[1] as Int
                val todayMin = args[2] as Int
                @Suppress("UNCHECKED_CAST")
                val streak = args[3] as Pair<Int, Int>
                @Suppress("UNCHECKED_CAST")
                val plants = args[4] as List<TodayGrownPlant>
                @Suppress("UNCHECKED_CAST")
                val missions = args[5] as List<HomeMissionItem>
                val isPremium = args[6] as Boolean
                val dailyBonusClaimed = args[7] as Boolean

                _uiState.update { state ->
                    state.copy(
                        seedBalance = seeds,
                        dailyGoalMinutes = goal,
                        todayFocusMinutes = todayMin,
                        currentStreak = streak.first,
                        retentionNudge = HomeRetentionNudge.from(todayMin, goal, streak.first),
                        todayGrownPlants = plants,
                        activeMissions = missions,
                        isPremium = isPremium,
                        dailySeedBonusClaimed = dailyBonusClaimed
                    )
                }
            }.collect {}
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectDuration -> selectDuration(intent.minutes)
            is HomeIntent.SelectTag -> _uiState.update { it.copy(selectedTag = intent.tag) }
            is HomeIntent.SelectPlant -> _uiState.update { it.copy(selectedPlantId = intent.plantId) }
            is HomeIntent.SelectSessionType -> _uiState.update { it.copy(sessionType = intent.type) }
            is HomeIntent.SetStrictFocus -> _uiState.update { it.copy(isStrict = intent.strict) }
            is HomeIntent.SelectAmbience -> _uiState.update { it.copy(ambienceSound = intent.sound) }
            HomeIntent.StartTodaySession -> startTodaySession()
            is HomeIntent.ClaimMission -> claimMission(intent.missionId)
            is HomeIntent.ClaimDailySeedBonus -> claimDailySeedBonus(intent.activity)
        }
    }

    private fun claimDailySeedBonus(activity: android.app.Activity?) {
        val state = _uiState.value
        if (state.dailySeedBonusClaimed) return
        if (state.isPremium) {
            viewModelScope.launch {
                val success = claimRewardedAdReward(
                    placement = "daily_seed_bonus",
                    sourceId = "premium",
                    nowMillis = clock.nowMillis()
                )
                if (success) {
                    effects.send(HomeEffect.MissionClaimed(50))
                } else {
                    effects.send(HomeEffect.ShowError("Hata: Ödül doğrulanamadı."))
                }
            }
        } else {
            if (activity == null) {
                viewModelScope.launch {
                    effects.send(HomeEffect.ShowError("Reklam yüklenemedi: Ekran hazır değil."))
                }
                return
            }
            rewardedAdManager.show(
                activity = activity,
                placement = RewardedAdManager.Placements.DAILY_SEED_BONUS,
                onRewardEarned = {
                    viewModelScope.launch {
                        val success = claimRewardedAdReward(
                            placement = "daily_seed_bonus",
                            sourceId = "ad",
                            nowMillis = clock.nowMillis()
                        )
                        if (success) {
                            effects.send(HomeEffect.MissionClaimed(50))
                        } else {
                            effects.send(HomeEffect.ShowError("Hata: Ödül doğrulanamadı."))
                        }
                    }
                },
                onNotReady = {
                    viewModelScope.launch {
                        effects.send(HomeEffect.ShowError("Reklam hazır değil. Lütfen daha sonra tekrar deneyin."))
                    }
                }
            )
        }
    }

    private fun selectDuration(minutes: Int) {
        if (minutes !in 5..180) return
        _uiState.update { state -> state.copy(selectedMinutes = minutes) }
    }

    private fun claimMission(missionId: String) {
        viewModelScope.launch {
            try {
                val earned = progressRepository.claimMissionReward(missionId, clock.nowMillis())
                if (earned > 0) {
                    effects.send(HomeEffect.MissionClaimed(earned))
                } else {
                    effects.send(HomeEffect.ShowError("Ödül zaten alınmış."))
                }
            } catch (e: Exception) {
                effects.send(HomeEffect.ShowError("Hata: ${e.localizedMessage}"))
            }
        }
    }

    private fun startTodaySession() {
        val state = _uiState.value
        if (state.isStartingSession) return

        _uiState.update { it.copy(isStartingSession = true) }
        viewModelScope.launch {
            try {
                val session = startSession(
                    plantId = state.selectedPlantId,
                    targetMinutes = if (state.sessionType == FocusSessionType.STOPWATCH) 0 else state.selectedMinutes,
                    selectedTag = state.selectedTag,
                    sessionType = state.sessionType,
                    isStrict = state.isStrict,
                    ambienceSound = state.ambienceSound,
                )
                effects.send(HomeEffect.OpenSession(
                    sessionId = session.id,
                    plantId = state.selectedPlantId,
                    minutes = if (state.sessionType == FocusSessionType.STOPWATCH) 0 else state.selectedMinutes,
                ))
            } finally {
                _uiState.update { it.copy(isStartingSession = false) }
            }
        }
    }
}
