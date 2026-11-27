package com.focusfarm.app.data.billing

import com.android.billingclient.api.Purchase

sealed interface PurchaseState {
    object Idle : PurchaseState
    object Loading : PurchaseState
    data class Success(val purchase: Purchase) : PurchaseState
    data class Error(val message: String) : PurchaseState
    object Canceled : PurchaseState
}
