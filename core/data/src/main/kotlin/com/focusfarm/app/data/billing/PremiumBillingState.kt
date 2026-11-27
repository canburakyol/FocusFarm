package com.focusfarm.app.data.billing

data class PremiumOffer(
    val plan: PremiumPlan,
    val title: String,
    val description: String,
    val price: String,
)

data class PremiumBillingState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val isPremiumUnlocked: Boolean = false,
    val offers: List<PremiumOffer> = emptyList(),
    val message: String? = null,
)
