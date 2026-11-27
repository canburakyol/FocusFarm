package com.focusfarm.app.monetization

import android.app.Activity
import android.content.Context
import com.focusfarm.app.data.billing.PremiumPlan
import com.focusfarm.app.ads.RewardedAdManager
import com.focusfarm.app.data.billing.BillingRepository
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
    private val billingRepository: BillingRepository,
    private val rewardedAdManager: RewardedAdManager,
    private val consentManager: ConsentManager,
) : MonetizationRepository {
    override val billingState: Flow<PremiumBillingState> = combine(
        billingRepository.entitlement,
        billingRepository.availableOffers,
        billingRepository.isLoading,
        billingRepository.errorMessages
    ) { entitlement, offers, isLoading, message ->
        PremiumBillingState(
            isConnected = true,
            isLoading = isLoading,
            isPremiumUnlocked = entitlement.isPremiumUnlocked,
            offers = offers,
            message = message
        )
    }

    override val canShowAds: Flow<Boolean> = combine(
        billingState,
        consentManager.state,
    ) { bState, consentState ->
        shouldShowAds(bState, consentState)
    }

    override fun refreshConsent(activity: Activity?) {
        consentManager.refreshConsent(activity)
    }

    override fun refreshBilling() {
        billingRepository.refresh()
    }

    override fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        billingRepository.launchPurchase(activity, plan)
    }

    override fun clearBillingMessage() {
        billingRepository.clearErrorMessage()
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
