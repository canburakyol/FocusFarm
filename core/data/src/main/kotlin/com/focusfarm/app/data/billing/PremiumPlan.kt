package com.focusfarm.app.data.billing

import com.android.billingclient.api.BillingClient

enum class PremiumPlan(
    val productId: String,
    val productType: String,
    val displayOrder: Int,
    val basePlanId: String? = null,
    val offerId: String? = null,
) {
    MONTHLY(
        productId = "premium_monthly",
        productType = BillingClient.ProductType.SUBS,
        displayOrder = 0,
        basePlanId = "monthly",
    ),
    YEARLY(
        productId = "premium_yearly",
        productType = BillingClient.ProductType.SUBS,
        displayOrder = 1,
        basePlanId = "yearly",
    ),
    LIFETIME(
        productId = "premium_lifetime",
        productType = BillingClient.ProductType.INAPP,
        displayOrder = 2,
    );

    companion object {
        fun fromProductId(productId: String): PremiumPlan? =
            entries.find { it.productId == productId }
    }
}
