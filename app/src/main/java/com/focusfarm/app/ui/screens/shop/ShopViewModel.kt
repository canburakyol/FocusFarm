package com.focusfarm.app.ui.screens.shop

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfarm.app.data.billing.PremiumBillingManager
import com.focusfarm.app.data.billing.PremiumBillingState
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.app.telemetry.AppTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val premiumBillingManager: PremiumBillingManager,
    private val telemetry: AppTelemetry,
) : ViewModel() {

    val billingState: StateFlow<PremiumBillingState> = premiumBillingManager.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PremiumBillingState(isLoading = true),
        )

    init {
        premiumBillingManager.start()
        premiumBillingManager.refresh()
        telemetry.logEvent("paywall_view")
    }

    fun buy(activity: Activity, plan: PremiumPlan) {
        telemetry.logEvent(
            name = "purchase_start",
            params = mapOf("product_id" to plan.productId),
        )
        premiumBillingManager.launchPurchase(activity, plan)
    }

    fun refresh() {
        premiumBillingManager.refresh()
    }

    fun clearMessage() {
        premiumBillingManager.clearMessage()
    }
}
