package com.focusfarm.app.monetization

import android.app.Activity
import android.content.Context
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.billing.PremiumBillingManager
import com.focusfarm.app.data.billing.PremiumBillingState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface MonetizationRepository {
    val billingState: Flow<PremiumBillingState>
    val canShowAds: Flow<Boolean>

    fun refreshConsent(activity: Activity? = null)

    fun refreshBilling()

    fun launchPurchase(activity: Activity, plan: PremiumPlan)

    fun clearBillingMessage()

    fun preloadRewarded(context: Context)

    fun showRewarded(
        activity: Activity,
        placement: String,
        onRewardEarned: () -> Unit,
        onNotReady: () -> Unit,
    )
}

internal fun shouldShowAds(
    billingState: PremiumBillingState,
    consentState: ConsentState,
): Boolean = consentState.canRequestAds && !billingState.isPremiumUnlocked

@Singleton
class DefaultMonetizationRepository @Inject constructor(
    private val premiumBillingManager: PremiumBillingManager,
    private val rewardedAdManager: RewardedAdManager,
    private val consentManager: ConsentManager,
) : MonetizationRepository {
    override val billingState: Flow<PremiumBillingState> = premiumBillingManager.state

    override val canShowAds: Flow<Boolean> = combine(
        premiumBillingManager.state,
        consentManager.state,
    ) { billingState, consentState ->
        shouldShowAds(billingState, consentState)
    }

    override fun refreshConsent(activity: Activity?) {
        consentManager.refreshConsent(activity)
    }

    override fun refreshBilling() {
        premiumBillingManager.refresh()
    }

    override fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        premiumBillingManager.launchPurchase(activity, plan)
    }

    override fun clearBillingMessage() {
        premiumBillingManager.clearMessage()
    }

    override fun preloadRewarded(context: Context) {
        if (!consentManager.canLoadAds()) return
        rewardedAdManager.preload(context)
    }

    override fun showRewarded(
        activity: Activity,
        placement: String,
        onRewardEarned: () -> Unit,
        onNotReady: () -> Unit,
    ) {
        if (!consentManager.canLoadAds()) {
            onNotReady()
            return
        }
        rewardedAdManager.show(
            activity = activity,
            placement = placement,
            onRewardEarned = onRewardEarned,
            onNotReady = onNotReady,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MonetizationRepositoryBindings {
    @Binds
    abstract fun bindMonetizationRepository(
        impl: DefaultMonetizationRepository,
    ): MonetizationRepository
}
