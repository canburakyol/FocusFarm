package com.focusfarm.app.data.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

object ProductDetailsMapper {
    fun mapToPremiumOffer(
        details: ProductDetails,
        plan: PremiumPlan
    ): PremiumOffer? {
        val titleText = details.title.ifBlank { plan.productId }
        val descriptionText = details.description

        if (plan.productType == BillingClient.ProductType.SUBS) {
            val offerDetails = details.subscriptionOfferDetails.orEmpty()
            val selectedOffer = selectConfiguredOffer(
                plan = plan,
                offers = offerDetails.map {
                    SubscriptionOfferRef(
                        basePlanId = it.basePlanId,
                        offerId = it.offerId
                    )
                }
            ) ?: return null

            val actualOfferDetails = offerDetails.firstOrNull {
                it.basePlanId == selectedOffer.basePlanId && it.offerId == selectedOffer.offerId
            } ?: return null

            val priceText = actualOfferDetails.pricingPhases.pricingPhaseList
                .lastOrNull()
                ?.formattedPrice
                ?: "-"

            return PremiumOffer(
                plan = plan,
                title = titleText,
                description = descriptionText,
                price = priceText
            )
        } else {
            val priceText = details.oneTimePurchaseOfferDetails?.formattedPrice ?: "-"
            return PremiumOffer(
                plan = plan,
                title = titleText,
                description = descriptionText,
                price = priceText
            )
        }
    }
}
