package com.focusfarm.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.focusfarm.app.telemetry.AppTelemetry
import com.focusfarm.core.ui.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Singleton
class PremiumBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetry: AppTelemetry,
) : PurchasesUpdatedListener {

    private data class CachedProduct(
        val details: ProductDetails,
        val offerToken: String?,
        val offer: PremiumOffer,
    )

    private val cachedProducts = mutableMapOf<PremiumPlan, CachedProduct>()

    private val _state = MutableStateFlow(PremiumBillingState(isLoading = true))
    val state: StateFlow<PremiumBillingState> = _state.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private var connecting = false

    init {
        start()
    }

    fun start() {
        if (billingClient.isReady) {
            _state.update { it.copy(isConnected = true) }
            refresh()
            return
        }
        if (connecting) return

        connecting = true
        _state.update { it.copy(isLoading = true) }

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        _state.update { it.copy(isConnected = true, message = null) }
                        refresh()
                    } else {
                        telemetry.recordNonFatal(
                            tag = "billing_setup_failed",
                            message = "code=${result.responseCode} message=${result.debugMessage}",
                        )
                        _state.update {
                            it.copy(
                                isConnected = false,
                                isLoading = false,
                                message = result.toUserMessage(),
                            )
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _state.update { it.copy(isConnected = false) }
                }
            }
        )
    }

    fun refresh() {
        if (!billingClient.isReady) {
            start()
            return
        }
        _state.update { it.copy(isLoading = true) }
        queryProducts()
        queryPurchases()
    }

    fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        if (!billingClient.isReady) {
            _state.update { it.copy(message = context.getString(R.string.billing_ready_error)) }
            start()
            return
        }

        val cached = cachedProducts[plan]
        if (cached == null) {
            _state.update { it.copy(message = context.getString(R.string.billing_load_error)) }
            refresh()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams
            .newBuilder()
            .setProductDetails(cached.details)

        cached.offerToken?.let { offerToken ->
            productDetailsParams.setOfferToken(offerToken)
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams.build()))
            .build()

        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update { it.copy(message = result.toUserMessage()) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun observeBillingState(): StateFlow<PremiumBillingState> = state

    fun observeIsPremium(): Flow<Boolean> = state.map { it.isPremiumUnlocked }

    fun purchase(planId: String) {
        val plan = PremiumPlan.entries.find { it.productId == planId } ?: return
        // Activity gerektirir, bu method PaywallScreen'den activity ile cagrilmali
        _state.update { it.copy(message = context.getString(R.string.billing_activity_required)) }
    }

    suspend fun restorePurchases() {
        if (!billingClient.isReady) {
            start()
            return
        }
        queryPurchases()
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                handlePurchases(purchases.orEmpty())
                trackSuccessfulPurchases(purchases.orEmpty())
                _state.update { it.copy(message = context.getString(R.string.billing_purchase_success)) }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update { it.copy(message = context.getString(R.string.billing_purchase_cancelled)) }
            }

            else -> {
                telemetry.recordNonFatal(
                    tag = "purchase_update_failed",
                    message = "code=${result.responseCode} message=${result.debugMessage}",
                )
                _state.update { it.copy(message = result.toUserMessage()) }
            }
        }
    }

    private fun queryProducts() {
        val plansByType = PremiumPlan.entries.groupBy { it.productType }
        if (plansByType.isEmpty()) {
            _state.update { it.copy(offers = emptyList(), message = null) }
            return
        }

        cachedProducts.clear()
        val allDetails = mutableListOf<ProductDetails>()
        var remainingCalls = plansByType.size
        var firstFailure: BillingResult? = null

        plansByType.forEach { (productType, plans) ->
            val products = plans.map { plan ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(plan.productId)
                    .setProductType(productType)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()

            billingClient.queryProductDetailsAsync(params) { result, queryResult ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    allDetails += queryResult.productDetailsList.orEmpty()
                } else {
                    if (firstFailure == null) {
                        firstFailure = result
                    }
                    telemetry.recordNonFatal(
                        tag = "query_products_failed",
                        message = "type=$productType code=${result.responseCode} message=${result.debugMessage}",
                    )
                }

                remainingCalls -= 1
                if (remainingCalls > 0) return@queryProductDetailsAsync

                allDetails
                    .mapNotNull { details -> details.toCachedProductOrNull() }
                    .forEach { (plan, cached) ->
                        cachedProducts[plan] = cached
                    }

                val offers = PremiumPlan.entries
                    .sortedBy(PremiumPlan::displayOrder)
                    .mapNotNull { cachedProducts[it]?.offer }
                val failure = firstFailure
                _state.update {
                    it.copy(
                        offers = offers,
                        message = if (offers.isEmpty() && failure != null) {
                            failure.toUserMessage()
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    private fun queryPurchases() {
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                val purchases = (subsPurchases + inAppPurchases)
                    .distinctBy { it.purchaseToken }

                handlePurchases(purchases)

                val hasError = subsResult.responseCode != BillingClient.BillingResponseCode.OK ||
                    inAppResult.responseCode != BillingClient.BillingResponseCode.OK

                _state.update {
                    it.copy(
                        isLoading = false,
                        message = if (hasError) {
                            listOf(subsResult.toUserMessage(), inAppResult.toUserMessage())
                                .firstOrNull { msg -> msg.isNotBlank() }
                        } else {
                            it.message
                        },
                    )
                }
            }
        }
    }

    private fun ProductDetails.toCachedProductOrNull(): Pair<PremiumPlan, CachedProduct>? {
        val plan = PremiumPlan.fromProductId(productId) ?: return null

        val titleText = title.ifBlank { plan.productId }
        val descriptionText = description

        return if (plan.productType == BillingClient.ProductType.SUBS) {
            val offer = selectConfiguredSubscriptionOffer(plan) ?: run {
                telemetry.recordNonFatal(
                    tag = "billing_offer_config_missing",
                    message = buildString {
                        append("productId=").append(productId)
                        append(" expectedBasePlan=").append(plan.basePlanId ?: "none")
                        append(" expectedOffer=").append(plan.offerId ?: "none")
                        append(" available=")
                        append(
                            subscriptionOfferDetails
                                ?.joinToString(separator = "|") { details ->
                                    "${details.basePlanId}:${details.offerId ?: "base"}"
                                }
                                ?: "none"
                        )
                    },
                )
                return null
            }
            val priceText = offer.pricingPhases.pricingPhaseList
                .lastOrNull()
                ?.formattedPrice
                ?: "-"

            val uiOffer = PremiumOffer(
                plan = plan,
                title = titleText,
                description = descriptionText,
                price = priceText,
            )

            plan to CachedProduct(
                details = this,
                offerToken = offer.offerToken,
                offer = uiOffer,
            )
        } else {
            val priceText = oneTimePurchaseOfferDetails?.formattedPrice ?: "-"

            val uiOffer = PremiumOffer(
                plan = plan,
                title = titleText,
                description = descriptionText,
                price = priceText,
            )

            plan to CachedProduct(
                details = this,
                offerToken = null,
                offer = uiOffer,
            )
        }
    }

    private fun ProductDetails.selectConfiguredSubscriptionOffer(
        plan: PremiumPlan,
    ): ProductDetails.SubscriptionOfferDetails? {
        val selected = selectConfiguredOffer(
            plan = plan,
            offers = subscriptionOfferDetails
                .orEmpty()
                .map { details ->
                    SubscriptionOfferRef(
                        basePlanId = details.basePlanId,
                        offerId = details.offerId,
                    )
                },
        ) ?: return null

        return subscriptionOfferDetails
            .orEmpty()
            .firstOrNull { details ->
                details.basePlanId == selected.basePlanId && details.offerId == selected.offerId
            }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var unlocked = false
        var hasPending = false

        purchases.forEach { purchase ->
            val containsPremium = purchase.products.any { PremiumPlan.fromProductId(it) != null }
            if (!containsPremium) return@forEach

            // SOTA: Kriptografik imza doğrulaması yap (Güvenlik Koruması)
            val isVerified = SecuritySignatureVerifier.isSignatureValid(
                context = context,
                signedData = purchase.originalJson,
                signature = purchase.signature
            )
            if (!isVerified) {
                telemetry.recordNonFatal(
                    tag = "billing_signature_failed",
                    message = "Signature verification failed for products: ${purchase.products}"
                )
                return@forEach
            }

            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    unlocked = true
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }

                Purchase.PurchaseState.PENDING -> {
                    hasPending = true
                }

                else -> Unit
            }
        }

        _state.update {
            it.copy(
                isPremiumUnlocked = unlocked,
                message = if (hasPending) {
                    context.getString(R.string.billing_pending_activation)
                } else {
                    it.message
                },
            )
        }
    }

    // SOTA: Yerel ödeme imza doğrulama yardımcısı
    private object SecuritySignatureVerifier {
        private const val BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzFocusFarmPlaceholderKey"

        fun verifyPurchase(publicKey: String, signedData: String, signature: String): Boolean {
            if (signedData.isEmpty() || signature.isEmpty() || publicKey.isEmpty()) {
                return false
            }
            return try {
                val keyBytes = android.util.Base64.decode(publicKey, android.util.Base64.DEFAULT)
                val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
                val keyFactory = java.security.KeyFactory.getInstance("RSA")
                val key = keyFactory.generatePublic(keySpec)

                val sig = java.security.Signature.getInstance("SHA1withRSA")
                sig.initVerify(key)
                sig.update(signedData.toByteArray())
                sig.verify(android.util.Base64.decode(signature, android.util.Base64.DEFAULT))
            } catch (e: Exception) {
                false
            }
        }

        fun isSignatureValid(context: Context, signedData: String, signature: String): Boolean {
            val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable && (BASE64_PUBLIC_KEY.isEmpty() || BASE64_PUBLIC_KEY.contains("Placeholder"))) {
                // Hata ayıklama derlemelerinde ve varsayılan anahtarla test etmeye izin ver
                return true
            }
            return verifyPurchase(BASE64_PUBLIC_KEY, signedData, signature)
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                telemetry.recordNonFatal(
                    tag = "acknowledge_purchase_failed",
                    message = "code=${result.responseCode} message=${result.debugMessage}",
                )
                _state.update { it.copy(message = result.toUserMessage()) }
            }
        }
    }

    private fun trackSuccessfulPurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return@forEach
            purchase.products
                .filter { productId -> PremiumPlan.fromProductId(productId) != null }
                .distinct()
                .forEach { productId ->
                    telemetry.logEvent(
                        name = "purchase_success",
                        params = mapOf("product_id" to productId),
                    )
                }
        }
    }

    private fun BillingResult.toUserMessage(): String {
        if (debugMessage.isNotBlank()) return debugMessage
        return when (responseCode) {
            BillingClient.BillingResponseCode.NETWORK_ERROR ->
                context.getString(R.string.billing_net_error)
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                context.getString(R.string.billing_service_unavailable)
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                context.getString(R.string.billing_unavailable)
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                context.getString(R.string.billing_item_unavailable)
            BillingClient.BillingResponseCode.USER_CANCELED ->
                context.getString(R.string.billing_purchase_cancelled)
            else -> context.getString(R.string.billing_purchase_failed)
        }
    }
}
