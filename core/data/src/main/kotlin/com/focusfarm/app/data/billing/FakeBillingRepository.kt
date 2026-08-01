package com.focusfarm.app.data.billing

import android.app.Activity
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBillingRepository(
    initialPremium: Boolean = false
) : BillingRepository {

    private val _entitlement = MutableStateFlow(
        PremiumEntitlement(
            isPremiumUnlocked = initialPremium,
            hasLifetime = initialPremium,
            hasMonthly = false,
            hasYearly = false,
            purchaseToken = if (initialPremium) "fake_token" else null,
            originalJson = null,
            signature = null
        )
    )
    override val entitlement: StateFlow<PremiumEntitlement> = _entitlement.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    override val purchaseState: Flow<PurchaseState> = _purchaseState

    private val _availableOffers = MutableStateFlow<List<PremiumOffer>>(
        listOf(
            PremiumOffer(
                plan = PremiumPlan.MONTHLY,
                title = "Aylık Premium",
                description = "Tüm premium bitki ve temalara 1 ay boyunca erişim sağla.",
                price = "29,99 TL"
            ),
            PremiumOffer(
                plan = PremiumPlan.YEARLY,
                title = "Yıllık Premium",
                description = "En popüler paket: Tüm özelliklere 1 yıl boyunca erişim sağla.",
                price = "199,99 TL"
            ),
            PremiumOffer(
                plan = PremiumPlan.LIFETIME,
                title = "Ömür Boyu Premium",
                description = "Tek seferlik ödeme: Tüm premium özelliklerin kilidini sonsuza dek aç.",
                price = "499,99 TL"
            )
        )
    )
    override val availableOffers: StateFlow<List<PremiumOffer>> = _availableOffers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String?>()
    override val errorMessages: Flow<String?> = _errorMessages.asSharedFlow()

    var simulateError: String? = null
    var simulateCancel: Boolean = false

    fun updatePremiumStatus(unlocked: Boolean, isLifetime: Boolean = true) {
        _entitlement.value = PremiumEntitlement(
            isPremiumUnlocked = unlocked,
            hasLifetime = isLifetime && unlocked,
            hasMonthly = !isLifetime && unlocked,
            hasYearly = false,
            purchaseToken = if (unlocked) "fake_token" else null,
            originalJson = null,
            signature = null
        )
    }

    override fun startConnection() {}

    override fun refresh() {}

    override fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        _isLoading.value = true
        _purchaseState.value = PurchaseState.Loading

        if (simulateError != null) {
            _purchaseState.value = PurchaseState.Error(simulateError!!)
            _isLoading.value = false
            return
        }

        if (simulateCancel) {
            _purchaseState.value = PurchaseState.Canceled
            _isLoading.value = false
            return
        }

        updatePremiumStatus(true, plan == PremiumPlan.LIFETIME)

        val mockJson = "{\"productId\":\"${plan.productId}\",\"purchaseToken\":\"fake_token_for_${plan.productId}\"}"
        val mockPurchase = Purchase(mockJson, "fake_signature")
        _purchaseState.value = PurchaseState.Success(mockPurchase)
        _isLoading.value = false
    }

    override fun restorePurchases() {
        updatePremiumStatus(true, true)
    }

    override fun clearErrorMessage() {}
}
