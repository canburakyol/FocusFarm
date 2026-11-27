package com.focusfarm.app.ui.screens.collection

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.R
import com.focusfarm.app.data.billing.BillingRepository
import com.focusfarm.app.data.local.InventoryDao
import com.focusfarm.app.data.repository.GardenRepository
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.domain.PlantSpecies
import com.focusfarm.app.domain.PlantSpeciesCatalog
import com.focusfarm.app.domain.PlantRarity
import com.focusfarm.app.domain.GardenPlantState
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
import com.focusfarm.app.ui.components.plantName

data class CollectionPlantUiState(
    val plant: PlantSpecies,
    val isUnlocked: Boolean,
    val isPremiumAccessUnlocked: Boolean,
    val grownCount: Int,
    val firstDiscoveredAt: Long?,
    val lastCompletedAt: Long?,
    val rewardSeeds: Int,
    val isRewardClaimed: Boolean,
) {
    val isDiscovered: Boolean
        get() = grownCount > 0

    val canClaimReward: Boolean
        get() = isDiscovered && !isRewardClaimed && isUnlocked
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    repository: GardenRepository,
    billingRepository: BillingRepository,
    customInventoryDao: InventoryDao,
    private val progressRepository: ProgressRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedRarity = MutableStateFlow<PlantRarity?>(null)
    val selectedRarity: StateFlow<PlantRarity?> = _selectedRarity.asStateFlow()

    private val gardenEntries = repository.getGardenEntries(com.focusfarm.app.domain.GardenFilter.ALL_TIME, hideWithered = false)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val isPremiumUnlocked: StateFlow<Boolean> = billingRepository.entitlement
        .map { it.isPremiumUnlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val claimedRewardPlantIds = progressRepository.observeClaimedCollectionRewardPlantIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val unlockedInventoryIds = customInventoryDao.observeAllItems()
        .map { list -> list.map { it.itemId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val collectionPlants: StateFlow<List<CollectionPlantUiState>> = combine(
        gardenEntries,
        isPremiumUnlocked,
        claimedRewardPlantIds,
        unlockedInventoryIds,
        _selectedRarity
    ) { entries, premiumUnlocked, claimedPlantIds, unlockedIds, rarity ->
        val bloomedEntries = entries.filter { it.state == GardenPlantState.BLOOMED }
        val byPlant = bloomedEntries.groupBy { it.plantSpeciesId }

        PlantSpeciesCatalog.ALL
            .filter { rarity == null || it.rarity == rarity }
            .map { plant ->
                val plantSessions = byPlant[plant.id].orEmpty()

                val isUnlocked = when {
                    !plant.isPremium && plant.unlockType == "free" -> true
                    !plant.isPremium && plant.unlockType == "shop" -> plant.id in unlockedIds
                    plant.isPremium -> premiumUnlocked || plant.id in unlockedIds
                    else -> false
                }

                CollectionPlantUiState(
                    plant = plant,
                    isUnlocked = isUnlocked,
                    isPremiumAccessUnlocked = !plant.isPremium || premiumUnlocked,
                    grownCount = plantSessions.size,
                    firstDiscoveredAt = plantSessions.minOfOrNull { it.plantedAt },
                    lastCompletedAt = plantSessions.maxOfOrNull { it.plantedAt },
                    rewardSeeds = rewardForPlant(plant),
                    isRewardClaimed = claimedPlantIds.contains(plant.id),
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filterRarity(rarity: PlantRarity?) {
        _selectedRarity.value = rarity
    }

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
                    context.plantName(target.plant.id, target.plant.displayName),
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

    private fun rewardForPlant(plant: PlantSpecies): Int =
        when {
            plant.isPremium -> 40 + (plant.minimumFocusMinutes / 5)
            else -> 20 + (plant.minimumFocusMinutes / 5)
        }
}
