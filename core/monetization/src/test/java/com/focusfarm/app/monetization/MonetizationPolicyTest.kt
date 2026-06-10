package com.focusfarm.app.monetization

import com.focusfarm.app.data.billing.PremiumBillingState
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.app.data.billing.SubscriptionOfferRef
import com.focusfarm.app.data.billing.selectConfiguredOffer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MonetizationPolicyTest {

    @Test
    fun shouldShowAdsIsFalseForPremiumUsers() {
        val result = shouldShowAds(
            billingState = PremiumBillingState(isPremiumUnlocked = true),
            consentState = ConsentState(canRequestAds = true),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun shouldShowAdsIsFalseWithoutConsent() {
        val result = shouldShowAds(
            billingState = PremiumBillingState(isPremiumUnlocked = false),
            consentState = ConsentState(canRequestAds = false),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun shouldShowAdsIsTrueForFreeUsersWithConsent() {
        val result = shouldShowAds(
            billingState = PremiumBillingState(isPremiumUnlocked = false),
            consentState = ConsentState(canRequestAds = true),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun selectConfiguredOfferUsesConfiguredBasePlanInsteadOfFirstItem() {
        val selected = selectConfiguredOffer(
            plan = PremiumPlan.MONTHLY,
            offers = listOf(
                SubscriptionOfferRef(basePlanId = "yearly", offerId = null),
                SubscriptionOfferRef(basePlanId = "monthly", offerId = null),
            ),
        )

        assertThat(selected).isEqualTo(SubscriptionOfferRef(basePlanId = "monthly", offerId = null))
    }

    @Test
    fun selectConfiguredOfferReturnsNullWhenConfiguredPlanMissing() {
        val selected = selectConfiguredOffer(
            plan = PremiumPlan.YEARLY,
            offers = listOf(
                SubscriptionOfferRef(basePlanId = "monthly", offerId = null),
            ),
        )

        assertThat(selected).isNull()
    }
}
