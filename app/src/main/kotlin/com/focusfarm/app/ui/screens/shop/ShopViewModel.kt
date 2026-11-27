package com.focusfarm.app.ui.screens.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.repository.UnlockedPlantsRepository
import com.focusfarm.app.data.local.UserInventoryDao
import com.focusfarm.app.data.repository.InsufficientSeedsException
import com.focusfarm.app.data.repository.ProgressRepository
import com.focusfarm.app.data.repository.PurchaseProductUseCase
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.domain.ShopCategory
import com.focusfarm.app.domain.ShopCatalog
import com.focusfarm.app.domain.ShopProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.app.Activity
import com.focusfarm.app.data.billing.BillingRepository
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.repository.ClaimRewardedAdRewardUseCase
import com.focusfarm.app.time.Clock
import kotlinx.coroutines.flow.map
import java.util.UUID

data class ShopItem(
    val product: ShopProduct,
    val isUnlocked: Boolean,
    val isPurchasing: Boolean = false,
)

data class StoreUiState(
    val seedBalance: Int = 0,
    val waterDroplets: Int = 0,
    val totalExperience: Int = 0,
    val selectedCategory: ShopCategory = ShopCategory.PLANT_SEED,
    val items: List<ShopItem> = emptyList(),
    val isPremium: Boolean = false,
)

sealed interface StoreIntent {
    data class ChangeCategory(val category: ShopCategory) : StoreIntent
    data class PurchaseProduct(val productId: String) : StoreIntent
    data class ClaimRewardedAdSeedPack(val activity: Activity?) : StoreIntent
}

sealed interface StoreEffect {
    data class ShowMessage(val message: String) : StoreEffect
}

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val inventoryDao: UserInventoryDao,
    private val unlockedPlantsRepository: UnlockedPlantsRepository,
    private val purchaseProduct: PurchaseProductUseCase,
    private val billingRepository: BillingRepository,
    private val rewardedAdManager: RewardedAdManager,
    private val claimRewardedAdReward: ClaimRewardedAdRewardUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val effects = Channel<StoreEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            inventoryDao.ensureExists()
            
            val isPremiumFlow = billingRepository.entitlement.map { it.isPremiumUnlocked }

            combine(
                progressRepository.observeSeeds(),
                inventoryDao.observe(),
                unlockedPlantsRepository.observeUnlockedPlantIds(),
                isPremiumFlow,
                _uiState
            ) { seeds, inventory, unlockedIds, isPremium, currentState ->

                // Only plants are sold: the other catalog categories have no effect in the app yet.
                val processedItems = ShopCatalog.PRODUCTS
                    .filter { it.category == ShopCategory.PLANT_SEED && it.id in PlantCatalog.SEED_SHOP_IDS }
                    .map { product ->
                        val isUnlocked = isPremium || product.id in unlockedIds
                        ShopItem(
                            product = product,
                            isUnlocked = isUnlocked,
                            isPurchasing = currentState.items.firstOrNull { it.product.id == product.id }?.isPurchasing ?: false
                        )
                    }

                StoreUiState(
                    seedBalance = seeds,
                    waterDroplets = inventory?.waterDroplets ?: 0,
                    totalExperience = inventory?.totalExperience ?: 0,
                    selectedCategory = currentState.selectedCategory,
                    items = processedItems,
                    isPremium = isPremium
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onIntent(intent: StoreIntent) {
        when (intent) {
            is StoreIntent.ChangeCategory -> {
                _uiState.update { it.copy(selectedCategory = intent.category) }
            }
            is StoreIntent.PurchaseProduct -> purchase(intent.productId)
            is StoreIntent.ClaimRewardedAdSeedPack -> claimRewardedAdSeedPack(intent.activity)
        }
    }

    private fun purchase(productId: String) {
        val item = _uiState.value.items.firstOrNull { it.product.id == productId } ?: return
        if (item.isUnlocked || item.isPurchasing) return

        _uiState.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.product.id == productId) it.copy(isPurchasing = true) else it
                }
            )
        }

        viewModelScope.launch {
            try {
                purchaseProduct(
                    itemType = item.product.category.name,
                    itemId = productId,
                    price = item.product.price
                )
                effects.send(StoreEffect.ShowMessage("${item.product.displayName} satın alındı!"))
            } catch (error: InsufficientSeedsException) {
                effects.send(
                    StoreEffect.ShowMessage(
                        "Yetersiz Tohum: ${error.required} gerekli, ${error.available} mevcut"
                    )
                )
            } catch (e: Exception) {
                effects.send(StoreEffect.ShowMessage("Satın alma sırasında hata oluştu: ${e.localizedMessage}"))
            } finally {
                _uiState.update { state ->
                    state.copy(
                        items = state.items.map {
                            if (it.product.id == productId) it.copy(isPurchasing = false) else it
                        }
                    )
                }
            }
        }
    }

    private fun claimRewardedAdSeedPack(activity: Activity?) {
        val state = _uiState.value
        if (state.isPremium) {
            viewModelScope.launch {
                effects.send(StoreEffect.ShowMessage("Premium üyeler reklam izleyemez."))
            }
            return
        }
        if (activity == null) {
            viewModelScope.launch {
                effects.send(StoreEffect.ShowMessage("Reklam yüklenemedi: Ekran hazır değil."))
            }
            return
        }

        rewardedAdManager.show(
            activity = activity,
            placement = RewardedAdManager.Placements.SHOP_SEED_PACK,
            onRewardEarned = {
                viewModelScope.launch {
                    val uniqueId = UUID.randomUUID().toString()
                    val success = claimRewardedAdReward(
                        placement = "shop_seed_pack",
                        sourceId = uniqueId,
                        nowMillis = clock.nowMillis()
                    )
                    if (success) {
                        effects.send(StoreEffect.ShowMessage("Tebrikler! +25 Seed cüzdanınıza eklendi. 🪙"))
                    } else {
                        effects.send(StoreEffect.ShowMessage("Hata: Ödül doğrulanamadı."))
                    }
                }
            },
            onNotReady = {
                viewModelScope.launch {
                    effects.send(StoreEffect.ShowMessage("Reklam hazır değil. Lütfen daha sonra tekrar deneyin."))
                }
            }
        )
    }
}
