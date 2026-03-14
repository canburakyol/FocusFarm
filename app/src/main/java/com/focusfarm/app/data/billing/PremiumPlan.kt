package com.focusfarm.app.data.billing

import com.android.billingclient.api.BillingClient

enum class PremiumPlan(
    val productId: String,
    val productType: String,
) {
    MONTHLY(
        productId = "premium_monthly",
        productType = BillingClient.ProductType.SUBS,
    ),
    YEARLY(
        productId = "premium_yearly",
        productType = BillingClient.ProductType.SUBS,
    ),
    LIFETIME(
        productId = "premium_lifetime",
        productType = BillingClient.ProductType.INAPP,
    );

    companion object {
        fun fromProductId(productId: String): PremiumPlan? =
            entries.find { it.productId == productId }
    }
}
