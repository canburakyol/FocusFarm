package com.focusfarm.app.data.billing

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    private val _errorMessages = MutableSharedFlow<String?>(extraBufferCapacity = 1)
    override val errorMessages: Flow<String?> = _errorMessages.asSharedFlow()

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

    init {
        _entitlement.value = getEntitlementOffline()
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
                handlePurchases(purchases.orEmpty())
                trackSuccessfulPurchases(purchases.orEmpty())
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

                _isLoading.value = false
                if (hasError) {
                    val errMsg = if (subsResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        subsResult.toUserMessage()
                    } else {
                        inAppResult.toUserMessage()
                    }
                    _errorMessages.tryEmit(errMsg)
                }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var unlocked = false
        var hasLifetime = false
        var hasMonthly = false
        var hasYearly = false
        var activePurchaseToken: String? = null
        var activeOriginalJson: String? = null
        var activeSignature: String? = null
        var hasPending = false
        var lastCompletedPurchase: Purchase? = null

        purchases.forEach { purchase ->
            val matchingPlan = purchase.products
                .mapNotNull { PremiumPlan.fromProductId(it) }
                .firstOrNull() ?: return@forEach

            val isVerified = SecuritySignatureVerifier.isSignatureValid(context, purchase.originalJson, purchase.signature)
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
                    activePurchaseToken = purchase.purchaseToken
                    activeOriginalJson = purchase.originalJson
                    activeSignature = purchase.signature
                    lastCompletedPurchase = purchase

                    when (matchingPlan) {
                        PremiumPlan.LIFETIME -> hasLifetime = true
                        PremiumPlan.MONTHLY -> hasMonthly = true
                        PremiumPlan.YEARLY -> hasYearly = true
                    }

                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    hasPending = true
                }
            }
        }

        val entitlement = PremiumEntitlement(
            isPremiumUnlocked = unlocked,
            hasLifetime = hasLifetime,
            hasMonthly = hasMonthly,
            hasYearly = hasYearly,
            purchaseToken = activePurchaseToken,
            originalJson = activeOriginalJson,
            signature = activeSignature
        )

        _entitlement.value = entitlement
        saveEntitlementOffline(entitlement)

        if (unlocked && lastCompletedPurchase != null) {
            _purchaseState.value = PurchaseState.Success(lastCompletedPurchase!!)
        }

        if (hasPending) {
            _errorMessages.tryEmit(context.getString(R.string.billing_pending_activation))
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

    private fun saveEntitlementOffline(entitlement: PremiumEntitlement) {
        val sharedPrefs = context.getSharedPreferences("secure_billing_cache", Context.MODE_PRIVATE)
        val salt = "FocusFarmPremiumSalt2026"
        val isPremiumVal = entitlement.isPremiumUnlocked
        val hasLifetimeVal = entitlement.hasLifetime
        val hasMonthlyVal = entitlement.hasMonthly
        val hasYearlyVal = entitlement.hasYearly
        val token = entitlement.purchaseToken ?: ""
        val dataString = "$isPremiumVal|$hasLifetimeVal|$hasMonthlyVal|$hasYearlyVal|$token|$salt"
        val checksum = sha256(dataString)

        sharedPrefs.edit {
            putBoolean("is_premium_unlocked", isPremiumVal)
            putBoolean("has_lifetime", hasLifetimeVal)
            putBoolean("has_monthly", hasMonthlyVal)
            putBoolean("has_yearly", hasYearlyVal)
            putString("purchase_token", token)
            putString("checksum", checksum)
        }
    }

    private fun getEntitlementOffline(): PremiumEntitlement {
        val sharedPrefs = context.getSharedPreferences("secure_billing_cache", Context.MODE_PRIVATE)
        val isPremiumVal = sharedPrefs.getBoolean("is_premium_unlocked", false)
        val hasLifetimeVal = sharedPrefs.getBoolean("has_lifetime", false)
        val hasMonthlyVal = sharedPrefs.getBoolean("has_monthly", false)
        val hasYearlyVal = sharedPrefs.getBoolean("has_yearly", false)
        val token = sharedPrefs.getString("purchase_token", "") ?: ""
        val checksum = sharedPrefs.getString("checksum", "") ?: ""

        val salt = "FocusFarmPremiumSalt2026"
        val expectedData = "$isPremiumVal|$hasLifetimeVal|$hasMonthlyVal|$hasYearlyVal|$token|$salt"
        val expectedChecksum = sha256(expectedData)

        if (checksum != expectedChecksum) {
            return PremiumEntitlement.NONE
        }

        return PremiumEntitlement(
            isPremiumUnlocked = isPremiumVal,
            hasLifetime = hasLifetimeVal,
            hasMonthly = hasMonthlyVal,
            hasYearly = hasYearlyVal,
            purchaseToken = token.ifEmpty { null },
            originalJson = null,
            signature = null
        )
    }

    private fun sha256(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

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
                return true
            }
            return verifyPurchase(BASE64_PUBLIC_KEY, signedData, signature)
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
