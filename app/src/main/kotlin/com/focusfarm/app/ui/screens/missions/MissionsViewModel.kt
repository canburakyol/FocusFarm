package com.focusfarm.app.ui.screens.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.domain.Mission
import com.focusfarm.app.domain.MissionProgress
import com.focusfarm.app.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.app.Activity
import com.focusfarm.app.data.billing.BillingRepository
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.repository.RewardGrantRepository
import com.focusfarm.app.data.repository.ClaimRewardedAdRewardUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

data class MissionUiItem(
    val mission: com.focusfarm.app.domain.Mission,
    val progress: com.focusfarm.app.domain.MissionProgress?,
)

data class MissionsUiState(
    val items: List<MissionUiItem> = emptyList(),
    val seedBalance: Int = 0,
    val isClaiming: Boolean = false,
    val isPremium: Boolean = false,
    val questRerollClaimed: Boolean = false,
)

sealed interface MissionsIntent {
    data class ClaimReward(val missionId: String) : MissionsIntent
    object Refresh : MissionsIntent
    data class RerollQuests(val activity: Activity?) : MissionsIntent
}

sealed interface MissionsEffect {
    data class RewardClaimed(val rewardSeeds: Int) : MissionsEffect
    data class ShowError(val message: String) : MissionsEffect
}

@HiltViewModel
class MissionsViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
    private val billingRepository: BillingRepository,
    private val rewardedAdManager: RewardedAdManager,
    private val rewardGrantRepository: RewardGrantRepository,
    private val claimRewardedAdReward: ClaimRewardedAdRewardUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionsUiState())
    val uiState: StateFlow<MissionsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MissionsEffect>(Channel.BUFFERED)
    val effect = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val todayDateKey = clock.today().toString()
            try {
                progressRepository.ensureMissionsForDate(todayDateKey, clock.nowMillis())
                progressRepository.updateMissionProgress(todayDateKey, clock.nowMillis())
            } catch (e: Exception) {
                // Ignore initial background setup errors
            }

            val missionsFlow = progressRepository.observeActiveMissions(clock.nowMillis())
            val seedsFlow = progressRepository.observeSeeds()
            val isPremiumFlow = billingRepository.entitlement.map { it.isPremiumUnlocked }
            val questRerollClaimedFlow = rewardGrantRepository.observeHasClaimedRewardToday("quest_reroll", todayDateKey)

            combine(
                missionsFlow,
                seedsFlow,
                isPremiumFlow,
                questRerollClaimedFlow
            ) { missions, seeds, isPremium, questRerollClaimed ->
                val missionIds = missions.map { it.id }
                progressRepository.observeMissionProgress(missionIds).collectLatest { progresses ->
                    val progressMap = progresses.associateBy { it.missionId }
                    val items = missions.map { mission ->
                        MissionUiItem(
                            mission = mission,
                            progress = progressMap[mission.id]
                        )
                    }
                    _uiState.update { it.copy(
                        items = items,
                        seedBalance = seeds,
                        isPremium = isPremium,
                        questRerollClaimed = questRerollClaimed
                    ) }
                }
            }.collect {}
        }
    }

    fun onIntent(intent: MissionsIntent) {
        when (intent) {
            is MissionsIntent.ClaimReward -> claimReward(intent.missionId)
            MissionsIntent.Refresh -> refresh()
            is MissionsIntent.RerollQuests -> rerollQuests(intent.activity)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val todayDateKey = clock.today().toString()
            progressRepository.updateMissionProgress(todayDateKey, clock.nowMillis())
        }
    }

    private fun claimReward(missionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClaiming = true) }
            try {
                val earned = progressRepository.claimMissionReward(missionId, clock.nowMillis())
                if (earned > 0) {
                    _effects.send(MissionsEffect.RewardClaimed(earned))
                } else {
                    _effects.send(MissionsEffect.ShowError("Ödül alınamadı veya zaten alındı."))
                }
            } catch (e: Exception) {
                _effects.send(MissionsEffect.ShowError("Hata: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(isClaiming = false) }
            }
        }
    }

    private fun rerollQuests(activity: Activity?) {
        val state = _uiState.value
        if (state.questRerollClaimed) return
        viewModelScope.launch {
            if (state.isPremium) {
                val success = claimRewardedAdReward(
                    placement = "quest_reroll",
                    sourceId = "premium",
                    nowMillis = clock.nowMillis()
                )
                if (success) {
                    val todayDateKey = clock.today().toString()
                    progressRepository.ensureMissionsForDate(todayDateKey, clock.nowMillis())
                    progressRepository.updateMissionProgress(todayDateKey, clock.nowMillis())
                    _effects.send(MissionsEffect.RewardClaimed(0))
                } else {
                    _effects.send(MissionsEffect.ShowError("Hata: Görevler yenilenemedi."))
                }
            } else {
                if (activity == null) {
                    _effects.send(MissionsEffect.ShowError("Reklam yüklenemedi: Ekran hazır değil."))
                    return@launch
                }
                rewardedAdManager.show(
                    activity = activity,
                    placement = RewardedAdManager.Placements.DAILY_QUEST_REROLL,
                    onRewardEarned = {
                        viewModelScope.launch {
                            val success = claimRewardedAdReward(
                                placement = "quest_reroll",
                                sourceId = "ad",
                                nowMillis = clock.nowMillis()
                            )
                            if (success) {
                                val todayDateKey = clock.today().toString()
                                progressRepository.ensureMissionsForDate(todayDateKey, clock.nowMillis())
                                progressRepository.updateMissionProgress(todayDateKey, clock.nowMillis())
                                _effects.send(MissionsEffect.RewardClaimed(0))
                            } else {
                                _effects.send(MissionsEffect.ShowError("Hata: Görevler yenilenemedi."))
                            }
                        }
                    },
                    onNotReady = {
                        viewModelScope.launch {
                            _effects.send(MissionsEffect.ShowError("Reklam hazır değil. Lütfen daha sonra tekrar deneyin."))
                        }
                    }
                )
            }
        }
    }
}
