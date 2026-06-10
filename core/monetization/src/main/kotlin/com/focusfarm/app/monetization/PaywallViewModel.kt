package com.focusfarm.app.monetization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.billing.PremiumBillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallPlan(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val isRecommended: Boolean,
    val savings: String? = null,
)

data class PaywallState(
    val isLoading: Boolean = false,
    val isTrialActive: Boolean = false,
    val trialDaysRemaining: Int = 0,
    val availablePlans: List<PaywallPlan> = emptyList(),
    val selectedPlanId: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: PremiumBillingManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = _state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaywallState())

    val isPremium: StateFlow<Boolean> = billingManager.observeIsPremium()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadPlans()
    }

    private fun loadPlans() {
        _state.update {
            it.copy(
                isLoading = true,
                availablePlans = listOf(
                    PaywallPlan(
                        id = "premium_monthly",
                        title = "Aylik",
                        description = "Her ay yenilenir",
                        price = "29,99 TL",
                        isRecommended = false,
                    ),
                    PaywallPlan(
                        id = "premium_yearly",
                        title = "Yillik",
                        description = "Her yil yenilenir",
                        price = "199,99 TL",
                        isRecommended = true,
                        savings = "%44 tasarruf",
                    ),
                    PaywallPlan(
                        id = "premium_lifetime",
                        title = "Omur Boyu",
                        description = "Tek odeme, sonsuza kadar",
                        price = "499,99 TL",
                        isRecommended = false,
                    ),
                ),
            )
        }

        viewModelScope.launch {
            try {
                billingManager.observeBillingState().collect { billingState ->
                    _state.update {
                        it.copy(
                            isLoading = billingState.isLoading,
                            errorMessage = billingState.message,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Baglanti hatasi: ${e.message}",
                    )
                }
            }
        }
    }

    fun selectPlan(planId: String) {
        _state.update { it.copy(selectedPlanId = planId) }
    }

    fun purchasePlan(planId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                billingManager.purchase(planId)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Satin alma basarisiz: ${e.message}",
                    )
                }
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            try {
                billingManager.restorePurchases()
            } catch (e: Exception) {
                _state.update {
                    it.copy(errorMessage = "Geri yukleme basarisiz: ${e.message}")
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
