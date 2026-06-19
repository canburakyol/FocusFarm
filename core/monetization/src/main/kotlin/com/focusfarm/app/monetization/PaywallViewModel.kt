package com.focusfarm.app.monetization

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.billing.PremiumBillingManager
import com.focusfarm.core.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
                        title = context.getString(R.string.plan_monthly),
                        description = context.getString(R.string.paywall_plan_monthly_desc),
                        price = "29,99 TL",
                        isRecommended = false,
                    ),
                    PaywallPlan(
                        id = "premium_yearly",
                        title = context.getString(R.string.plan_yearly),
                        description = context.getString(R.string.paywall_plan_yearly_desc),
                        price = "199,99 TL",
                        isRecommended = true,
                        savings = context.getString(R.string.paywall_plan_yearly_savings),
                    ),
                    PaywallPlan(
                        id = "premium_lifetime",
                        title = context.getString(R.string.plan_lifetime),
                        description = context.getString(R.string.paywall_plan_lifetime_desc),
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
                        errorMessage = context.getString(
                            R.string.paywall_error_connection,
                            e.message.orEmpty(),
                        ),
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
                        errorMessage = context.getString(
                            R.string.paywall_error_purchase_failed_with_message,
                            e.message.orEmpty(),
                        ),
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
                    it.copy(
                        errorMessage = context.getString(
                            R.string.paywall_error_restore_failed_with_message,
                            e.message.orEmpty(),
                        ),
                    )
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
