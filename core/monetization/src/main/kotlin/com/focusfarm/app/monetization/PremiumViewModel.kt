package com.focusfarm.app.monetization

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.billing.BillingRepository
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.app.data.billing.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _selectedPlanId = MutableStateFlow<String?>(null)

    val state: StateFlow<BillingUiState> = combine(
        billingRepository.isLoading,
        billingRepository.entitlement,
        billingRepository.availableOffers,
        billingRepository.purchaseState,
        billingRepository.errorMessages,
        _selectedPlanId
    ) { args: Array<Any?> ->
        val isLoading = args[0] as Boolean
        val entitlement = args[1] as com.focusfarm.app.data.billing.PremiumEntitlement
        val offers = args[2] as List<com.focusfarm.app.data.billing.PremiumOffer>
        val purchaseState = args[3] as com.focusfarm.app.data.billing.PurchaseState
        val errorMessage = args[4] as String?
        val selectedPlanId = args[5] as String?

        val finalSelectedId = selectedPlanId ?: offers.find { it.plan == PremiumPlan.YEARLY }?.plan?.productId ?: offers.firstOrNull()?.plan?.productId

        BillingUiState(
            isLoading = isLoading,
            isPremiumUnlocked = entitlement.isPremiumUnlocked,
            offers = offers,
            selectedPlanId = finalSelectedId,
            purchaseState = purchaseState,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BillingUiState(isLoading = true)
    )

    fun selectPlan(planId: String) {
        _selectedPlanId.value = planId
    }

    fun purchasePlan(activity: Activity, planId: String) {
        val plan = PremiumPlan.fromProductId(planId) ?: return
        viewModelScope.launch {
            billingRepository.launchPurchase(activity, plan)
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            billingRepository.restorePurchases()
        }
    }

    fun dismissError() {
        billingRepository.clearErrorMessage()
    }
}
