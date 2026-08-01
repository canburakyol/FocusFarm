package com.focusfarm.app.monetization

import com.focusfarm.app.data.billing.PremiumOffer
import com.focusfarm.app.data.billing.PurchaseState

data class BillingUiState(
    val isLoading: Boolean = false,
    val isPremiumUnlocked: Boolean = false,
    val offers: List<PremiumOffer> = emptyList(),
    val selectedPlanId: String? = null,
    val purchaseState: PurchaseState = PurchaseState.Idle,
    val errorMessage: String? = null
)
