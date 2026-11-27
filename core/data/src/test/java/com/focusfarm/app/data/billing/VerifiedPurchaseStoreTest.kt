package com.focusfarm.app.data.billing

import com.android.billingclient.api.BillingClient
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VerifiedPurchaseStoreTest {
    private val ok = BillingClient.BillingResponseCode.OK
    private val failure = BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
    private val monthly = purchase(PremiumPlan.MONTHLY, "monthly")
    private val lifetime = purchase(PremiumPlan.LIFETIME, "lifetime")

    @Test
    fun bothSuccessfulQueriesCombineAndEmptySnapshotsRevokeOnlyTheirOwnType() {
        val store = VerifiedPurchaseStore()
        assertThat(store.applyQueryResult(BillingClient.ProductType.SUBS, ok, listOf(monthly))).isTrue()
        assertThat(store.applyQueryResult(BillingClient.ProductType.INAPP, ok, listOf(lifetime))).isTrue()
        assertThat(store.entitlement().hasMonthly).isTrue()
        assertThat(store.entitlement().hasLifetime).isTrue()

        store.applyQueryResult(BillingClient.ProductType.SUBS, ok, emptyList())
        assertThat(store.entitlement().hasMonthly).isFalse()
        assertThat(store.entitlement().hasLifetime).isTrue()
        store.applyQueryResult(BillingClient.ProductType.INAPP, ok, emptyList())
        assertThat(store.entitlement()).isEqualTo(PremiumEntitlement.NONE)
    }

    @Test
    fun failedQueriesDoNotErasePriorVerificationEvenWhenOtherTypeIsEmpty() {
        val store = VerifiedPurchaseStore()
        store.merge(listOf(monthly, lifetime)) // Models records already validated by the repository.

        assertThat(store.applyQueryResult(BillingClient.ProductType.SUBS, failure, emptyList())).isFalse()
        assertThat(store.applyQueryResult(BillingClient.ProductType.INAPP, ok, emptyList())).isTrue()
        assertThat(store.entitlement().hasMonthly).isTrue()
        assertThat(store.entitlement().hasLifetime).isFalse()

        assertThat(store.applyQueryResult(BillingClient.ProductType.INAPP, failure, emptyList())).isFalse()
        assertThat(store.entitlement().hasMonthly).isTrue()
        assertThat(store.entitlement().isPremiumUnlocked).isTrue()
    }

    @Test
    fun bothFailedQueriesAndOfflineStartupKeepRestoredVerification() {
        val store = VerifiedPurchaseStore()
        store.merge(listOf(lifetime))
        store.applyQueryResult(BillingClient.ProductType.SUBS, failure, emptyList())
        store.applyQueryResult(BillingClient.ProductType.INAPP, failure, emptyList())
        assertThat(store.entitlement().hasLifetime).isTrue()
    }

    @Test
    fun successfulRestoreAndPurchaseCallbackMergeAreReconciledByLaterQuery() {
        val store = VerifiedPurchaseStore()
        store.merge(listOf(monthly))
        store.merge(listOf(lifetime))
        assertThat(store.entitlement().isPremiumUnlocked).isTrue()
        store.applyQueryResult(BillingClient.ProductType.SUBS, ok, emptyList())
        assertThat(store.entitlement().hasLifetime).isTrue()
        store.applyQueryResult(BillingClient.ProductType.INAPP, ok, emptyList())
        assertThat(store.entitlement().isPremiumUnlocked).isFalse()
    }

    private fun purchase(plan: PremiumPlan, token: String) =
        VerifiedPurchase(plan, token, "signed purchase $token", "signature $token")
}
