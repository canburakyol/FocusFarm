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
import com.focusfarm.core.data.BuildConfig
import com.focusfarm.core.ui.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DefaultBillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetry: AppTelemetry,
) : BillingRepository, PurchasesUpdatedListener {

    private val _entitlement = MutableStateFlow(PremiumEntitlement.NONE)
    override val entitlement: StateFlow<PremiumEntitlement> = _entitlement.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    override val purchaseState: Flow<PurchaseState> = _purchaseState

    private val _availableOffers = MutableStateFlow<List<PremiumOffer>>(emptyList())
    override val availableOffers: StateFlow<List<PremiumOffer>> = _availableOffers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessages = MutableStateFlow<String?>(null)
    override val errorMessages: Flow<String?> = _errorMessages.asStateFlow()

    private val cachedProductDetails = mutableMapOf<PremiumPlan, ProductDetails>()
    private val cachedOfferTokens = mutableMapOf<PremiumPlan, String?>()

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
    private val verifiedPurchases = VerifiedPurchaseStore()
    private var purchaseQueryGeneration = 0L
    private val acknowledgementTokens = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        loadVerifiedPurchases()
        startConnection()
    }

    override fun startConnection() {
        if (billingClient.isReady) {
            refresh()
            return
        }
        if (connecting) return

        connecting = true
        _isLoading.value = true

        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        refresh()
                    } else {
                        telemetry.recordNonFatal(
                            tag = "billing_setup_failed",
                            message = "code=${result.responseCode} message=${result.debugMessage}",
                        )
                        _isLoading.value = false
                        _errorMessages.tryEmit(result.toUserMessage())
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isLoading.value = false
                }
            }
        )
    }

    override fun refresh() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        _isLoading.value = true
        queryProducts()
        queryPurchases()
    }

    override fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        if (!billingClient.isReady) {
            _errorMessages.tryEmit(context.getString(R.string.billing_ready_error))
            startConnection()
            return
        }

        val details = cachedProductDetails[plan]
        if (details == null) {
            _errorMessages.tryEmit(context.getString(R.string.billing_load_error))
            refresh()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        cachedOfferTokens[plan]?.let { offerToken ->
            productDetailsParams.setOfferToken(offerToken)
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams.build()))
            .build()

        _purchaseState.value = PurchaseState.Loading
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseState.Error(result.toUserMessage())
            _errorMessages.tryEmit(result.toUserMessage())
        }
    }

    override fun restorePurchases() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        _isLoading.value = true
        queryPurchases()
    }

    override fun clearErrorMessage() {
        _errorMessages.tryEmit(null)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val valid = processPurchases(purchases.orEmpty())
                if (valid.isNotEmpty()) {
                    verifiedPurchases.merge(valid.map { it.second })
                    publishEntitlement()
                    valid.map { it.first }.filterNot { it.isAcknowledged }.forEach(::acknowledgePurchase)
                    _purchaseState.value = PurchaseState.Success(valid.last().first)
                    trackSuccessfulPurchases(valid.map { it.first })
                }
                // Purchase updates are deltas; reconcile both complete product snapshots.
                if (billingClient.isReady) queryPurchases()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Canceled
                _errorMessages.tryEmit(context.getString(R.string.billing_purchase_cancelled))
            }
            else -> {
                telemetry.recordNonFatal(
                    tag = "purchase_update_failed",
                    message = "code=${result.responseCode} message=${result.debugMessage}",
                )
                val errMsg = result.toUserMessage()
                _purchaseState.value = PurchaseState.Error(errMsg)
                _errorMessages.tryEmit(errMsg)
            }
        }
    }

    private fun queryProducts() {
        val plansByType = PremiumPlan.entries.groupBy { it.productType }
        if (plansByType.isEmpty()) {
            _availableOffers.value = emptyList()
            return
        }

        cachedProductDetails.clear()
        cachedOfferTokens.clear()
        val allOffers = mutableListOf<PremiumOffer>()
        var remainingCalls = plansByType.size

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
                    queryResult.productDetailsList.orEmpty().forEach { details ->
                        val plan = PremiumPlan.fromProductId(details.productId)
                        if (plan != null) {
                            cachedProductDetails[plan] = details

                            val offer = ProductDetailsMapper.mapToPremiumOffer(details, plan)
                            if (offer != null) {
                                allOffers.add(offer)

                                if (plan.productType == BillingClient.ProductType.SUBS) {
                                    val selectedOffer = selectConfiguredSubscriptionOffer(plan, details)
                                    cachedOfferTokens[plan] = selectedOffer?.offerToken
                                }
                            }
                        }
                    }
                } else {
                    telemetry.recordNonFatal(
                        tag = "query_products_failed",
                        message = "type=$productType code=${result.responseCode} message=${result.debugMessage}",
                    )
                }

                remainingCalls -= 1
                if (remainingCalls == 0) {
                    val sortedOffers = PremiumPlan.entries
                        .sortedBy(PremiumPlan::displayOrder)
                        .mapNotNull { plan -> allOffers.find { it.plan == plan } }
                    _availableOffers.value = sortedOffers
                    _isLoading.value = false
                }
            }
        }
    }

    private fun selectConfiguredSubscriptionOffer(
        plan: PremiumPlan,
        details: ProductDetails
    ): ProductDetails.SubscriptionOfferDetails? {
        val selected = selectConfiguredOffer(
            plan = plan,
            offers = details.subscriptionOfferDetails.orEmpty().map {
                SubscriptionOfferRef(
                    basePlanId = it.basePlanId,
                    offerId = it.offerId
                )
            }
        ) ?: return null

        return details.subscriptionOfferDetails.orEmpty().firstOrNull {
            it.basePlanId == selected.basePlanId && it.offerId == selected.offerId
        }
    }

    private fun queryPurchases() {
        val generation = ++purchaseQueryGeneration
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) subsQuery@{ subsResult, subsPurchases ->
            if (generation != purchaseQueryGeneration) return@subsQuery
            applyPurchaseQueryResult(BillingClient.ProductType.SUBS, subsResult, subsPurchases)
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(inAppParams) inAppQuery@{ inAppResult, inAppPurchases ->
                if (generation != purchaseQueryGeneration) return@inAppQuery
                applyPurchaseQueryResult(BillingClient.ProductType.INAPP, inAppResult, inAppPurchases)
                _isLoading.value = false
            }
        }
    }

    private fun applyPurchaseQueryResult(type: String, result: BillingResult, purchases: List<Purchase>) {
        val valid = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            processPurchases(purchases).filter { it.second.plan.productType == type }
        } else {
            emptyList()
        }
        if (!verifiedPurchases.applyQueryResult(type, result.responseCode, valid.map { it.second })) {
            telemetry.recordNonFatal(
                tag = "query_purchases_failed",
                message = "type=$type code=${result.responseCode} message=${result.debugMessage}",
            )
            _errorMessages.tryEmit(result.toUserMessage())
            return
        }
        publishEntitlement()
        valid.map { it.first }.filterNot { it.isAcknowledged }.forEach(::acknowledgePurchase)
    }

    private fun processPurchases(purchases: List<Purchase>): List<Pair<Purchase, VerifiedPurchase>> {
        var pending = false
        val valid = purchases.mapNotNull { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                pending = true
                return@mapNotNull null
            }
            val record = verifiedPurchase(purchase) ?: return@mapNotNull null
            purchase to record
        }
        if (pending) _errorMessages.tryEmit(context.getString(R.string.billing_pending_activation))
        return valid
    }

    private fun verifiedPurchase(purchase: Purchase): VerifiedPurchase? {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return null
        val plan = purchase.products.mapNotNull(PremiumPlan::fromProductId).firstOrNull() ?: return null
        if (!PurchaseSignatureVerifier.verify(
                BuildConfig.BILLING_PUBLIC_KEY, purchase.originalJson, purchase.signature,
            )) {
            telemetry.recordNonFatal(
                tag = "billing_signature_failed",
                message = "Signature verification failed for products: ${purchase.products}",
            )
            return null
        }
        return VerifiedPurchase(plan, purchase.purchaseToken, purchase.originalJson, purchase.signature)
    }

    private fun publishEntitlement() {
        _entitlement.value = verifiedPurchases.entitlement()
        val json = JSONArray()
        verifiedPurchases.snapshot().forEach { purchase ->
            json.put(JSONObject().put("purchase", purchase.originalJson).put("signature", purchase.signature))
        }
        context.getSharedPreferences(VERIFIED_PURCHASES_PREFS, Context.MODE_PRIVATE)
            .edit().putString(VERIFIED_PURCHASES_KEY, json.toString()).apply()
    }

    private fun loadVerifiedPurchases() {
        val saved = context.getSharedPreferences(VERIFIED_PURCHASES_PREFS, Context.MODE_PRIVATE)
            .getString(VERIFIED_PURCHASES_KEY, null) ?: return
        val records = try {
            val json = JSONArray(saved)
            (0 until json.length()).mapNotNull { index ->
                val entry = json.getJSONObject(index)
                val purchase = Purchase(entry.getString("purchase"), entry.getString("signature"))
                verifiedPurchase(purchase)
            }
        } catch (error: Exception) {
            telemetry.recordNonFatal(tag = "billing_cache_invalid", message = error.message.orEmpty())
            emptyList()
        }
        verifiedPurchases.merge(records)
        _entitlement.value = verifiedPurchases.entitlement()
    }

    private companion object {
        const val VERIFIED_PURCHASES_PREFS = "verified_billing_purchases"
        const val VERIFIED_PURCHASES_KEY = "purchases_v1"
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (!acknowledgementTokens.add(purchase.purchaseToken)) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                acknowledgementTokens.remove(purchase.purchaseToken)
                telemetry.recordNonFatal(
                    tag = "acknowledge_purchase_failed",
                    message = "code=${result.responseCode} message=${result.debugMessage}",
                )
                _errorMessages.tryEmit(result.toUserMessage())
            }
        }
    }

    private fun trackSuccessfulPurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return@forEach
            purchase.products
                .filter { PremiumPlan.fromProductId(it) != null }
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
