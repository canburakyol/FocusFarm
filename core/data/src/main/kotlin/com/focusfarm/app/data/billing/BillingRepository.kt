package com.focusfarm.app.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val entitlement: StateFlow<PremiumEntitlement>
    val purchaseState: Flow<PurchaseState>
    val availableOffers: StateFlow<List<PremiumOffer>>
    val isLoading: StateFlow<Boolean>
    val errorMessages: Flow<String?>

    fun startConnection()
    fun refresh()
    fun launchPurchase(activity: Activity, plan: PremiumPlan)
    fun restorePurchases()
    fun clearErrorMessage()
}
