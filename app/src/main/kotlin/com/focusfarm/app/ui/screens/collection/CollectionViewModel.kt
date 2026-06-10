package com.focusfarm.app.ui.screens.collection

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.R
import com.focusfarm.app.data.billing.PremiumBillingManager
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.domain.FocusSession
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionPlantUiState(
    val plant: Plant,
    val isPremiumAccessUnlocked: Boolean,
    val discoveredCount: Int,
    val lastCompletedAt: Long?,
    val rewardSeeds: Int,
    val isRewardClaimed: Boolean,
) {
    val isDiscovered: Boolean
        get() = discoveredCount > 0

    val canClaimReward: Boolean
        get() = isDiscovered && !isRewardClaimed && isPremiumAccessUnlocked
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    repository: GardenRepository,
    premiumBillingManager: PremiumBillingManager,
    private val progressRepository: ProgressRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val allSessions: StateFlow<List<FocusSession>> = repository
        .getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val isPremiumUnlocked: StateFlow<Boolean> = premiumBillingManager.state
        .map { it.isPremiumUnlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val claimedRewardPlantIds = progressRepository.observeClaimedCollectionRewardPlantIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val collectionPlants: StateFlow<List<CollectionPlantUiState>> = combine(
        allSessions,
        isPremiumUnlocked,
        claimedRewardPlantIds,
    ) { sessions, premiumUnlocked, claimedPlantIds ->
        val byPlant = sessions.groupBy { it.plantId }
        PlantCatalog.ALL.map { plant ->
            val plantSessions = byPlant[plant.id].orEmpty()
            CollectionPlantUiState(
                plant = plant,
                isPremiumAccessUnlocked = !plant.isPremium || premiumUnlocked,
                discoveredCount = plantSessions.size,
                lastCompletedAt = plantSessions.maxOfOrNull { it.completedAt },
                rewardSeeds = rewardForPlant(plant),
                isRewardClaimed = claimedPlantIds.contains(plant.id),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun claimReward(plantId: String) {
        viewModelScope.launch {
            val target = collectionPlants.value.firstOrNull { it.plant.id == plantId } ?: return@launch
            if (!target.canClaimReward) {
                _statusMessage.value = context.getString(R.string.status_collection_reward_unavailable)
                return@launch
            }
            val success = progressRepository.claimCollectionReward(
                plantId = plantId,
                rewardSeeds = target.rewardSeeds,
            )
            _statusMessage.value = if (success) {
                context.getString(
                    R.string.status_collection_reward_claimed,
                    target.plant.name,
                    target.rewardSeeds,
                )
            } else {
                context.getString(R.string.status_collection_reward_already_claimed)
            }
        }
    }

    fun dismissMessage() {
        _statusMessage.value = null
    }

    private fun rewardForPlant(plant: Plant): Int =
        when {
            plant.isPremium -> 40 + (plant.minMinutes / 5)
            else -> 20 + (plant.minMinutes / 5)
        }
}
