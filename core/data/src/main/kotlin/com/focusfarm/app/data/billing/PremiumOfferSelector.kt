package com.focusfarm.app.data.billing

data class SubscriptionOfferRef(
    val basePlanId: String,
    val offerId: String?,
)

fun selectConfiguredOffer(
    plan: PremiumPlan,
    offers: List<SubscriptionOfferRef>,
): SubscriptionOfferRef? {
    val configuredBasePlanId = plan.basePlanId ?: return null
    val configuredOfferId = plan.offerId

    return offers
        .asSequence()
        .filter { offer -> offer.basePlanId == configuredBasePlanId }
        .filter { offer -> configuredOfferId == null || offer.offerId == configuredOfferId }
        .sortedWith(compareBy<SubscriptionOfferRef> { it.basePlanId }.thenBy { it.offerId ?: "" })
        .firstOrNull()
}
