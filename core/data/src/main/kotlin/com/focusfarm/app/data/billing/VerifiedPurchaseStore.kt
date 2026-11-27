package com.focusfarm.app.data.billing

import com.android.billingclient.api.BillingClient

internal data class VerifiedPurchase(
    val plan: PremiumPlan,
    val token: String,
    val originalJson: String,
    val signature: String,
)

/** Holds only purchases whose PURCHASED state and signature were checked by the caller. */
internal class VerifiedPurchaseStore {
    private val byType = mutableMapOf<String, List<VerifiedPurchase>>()

    /** A failed query is not a completed snapshot and must leave this type untouched. */
    fun applyQueryResult(type: String, responseCode: Int, purchases: List<VerifiedPurchase>): Boolean {
        if (responseCode != BillingClient.BillingResponseCode.OK) return false
        replace(type, purchases)
        return true
    }

    fun replace(type: String, purchases: List<VerifiedPurchase>) {
        require(type == BillingClient.ProductType.SUBS || type == BillingClient.ProductType.INAPP)
        require(purchases.all { it.plan.productType == type })
        byType[type] = purchases.distinctBy(VerifiedPurchase::token)
    }

    fun merge(purchases: List<VerifiedPurchase>) {
        purchases.groupBy { it.plan.productType }.forEach { (type, additions) ->
            replace(type, (byType[type].orEmpty() + additions).asReversed()
                .distinctBy(VerifiedPurchase::token).asReversed())
        }
    }

    fun snapshot(): List<VerifiedPurchase> =
        byType[BillingClient.ProductType.SUBS].orEmpty() +
            byType[BillingClient.ProductType.INAPP].orEmpty()

    fun entitlement(): PremiumEntitlement {
        val purchases = snapshot()
        val selected = purchases.lastOrNull()
        return PremiumEntitlement(
            isPremiumUnlocked = purchases.isNotEmpty(),
            hasLifetime = purchases.any { it.plan == PremiumPlan.LIFETIME },
            hasMonthly = purchases.any { it.plan == PremiumPlan.MONTHLY },
            hasYearly = purchases.any { it.plan == PremiumPlan.YEARLY },
            purchaseToken = selected?.token,
            originalJson = selected?.originalJson,
            signature = selected?.signature,
        )
    }
}
