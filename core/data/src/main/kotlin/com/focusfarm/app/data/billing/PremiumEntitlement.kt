package com.focusfarm.app.data.billing

data class PremiumEntitlement(
    val isPremiumUnlocked: Boolean,
    val hasLifetime: Boolean,
    val hasMonthly: Boolean,
    val hasYearly: Boolean,
    val purchaseToken: String?,
    val originalJson: String?,
    val signature: String?
) {
    companion object {
        val NONE = PremiumEntitlement(
            isPremiumUnlocked = false,
            hasLifetime = false,
            hasMonthly = false,
            hasYearly = false,
            purchaseToken = null,
            originalJson = null,
            signature = null
        )
    }
}
