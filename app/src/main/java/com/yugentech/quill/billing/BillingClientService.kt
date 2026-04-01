package com.yugentech.quill.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.yugentech.quill.domain.BillingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class BillingClientService(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _subProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val subProducts = _subProducts.asStateFlow()

    private val _tipProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val tipProducts = _tipProducts.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>()
    val events = _events.asSharedFlow()

    private val _isPro = MutableStateFlow(false)
    val isPro = _isPro.asStateFlow()

    private var currentUserId: String? = null

    /**
     * Updates the local user context. Call this whenever the user logs in or logs out.
     */
    fun setCurrentUser(userId: String?) {
        currentUserId = userId
        Timber.d("Billing user context set to: $userId")
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases?.forEach { scope.launch { handlePurchase(it) } }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                scope.launch { _events.emit(BillingEvent.UserCancelled) }

            // ADD THIS BLOCK CATCHING "ITEM_ALREADY_OWNED"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Timber.w("User attempted to buy, but Play Store account already owns it.")
                scope.launch {
                    _events.emit(
                        BillingEvent.Error(
                            "This Google Play account is already subscribed. To subscribe on this new Quill profile, please switch to a different Google account in the Play Store app."
                        )
                    )
                }
            }

            else -> {
                Timber.e("Purchase error [${result.responseCode}]: ${result.debugMessage}")
                // Fallback for network errors, declined cards, etc.
                val errorMessage = result.debugMessage.ifBlank {
                    "An error occurred with Google Play. Please try again."
                }
                scope.launch { _events.emit(BillingEvent.Error(errorMessage)) }
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── 1. connect ────────────────────────────────────────────────────────────

    /**
     * Establishes connection to Google Play. If a user is already logged in,
     * it will automatically trigger a restore check.
     */
    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingClient connected")
                    scope.launch {
                        querySubProducts()
                        queryTipProducts()

                        // If we already have a user context, sync their Pro status immediately
                        currentUserId?.let { restorePurchases(it) }
                    }
                } else {
                    Timber.e("BillingClient setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.w("BillingClient disconnected")
            }
        })
    }

    // ── 2. queryProductDetails ────────────────────────────────────────────────

    private suspend fun querySubProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductIds.subs.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val (result, products) = billingClient.queryProductDetails(params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _subProducts.value = products ?: emptyList()
        }
    }

    private suspend fun queryTipProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductIds.tips.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        val (result, products) = billingClient.queryProductDetails(params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _tipProducts.value = products ?: emptyList()
        }
    }

    // ── 3. launch flows ───────────────────────────────────────────────────────

    // ── 3. launch flows ───────────────────────────────────────────────────────

    fun launchSubscriptionFlow(activity: Activity, basePlanId: String, userId: String) {
        scope.launch {
            // 1. PRE-FLIGHT CHECK: Ask Google if this device already owns the sub
            val queryParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            val (result, purchases) = billingClient.queryPurchasesAsync(queryParams)

            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val existingSub = purchases.find { purchase ->
                    purchase.purchaseState == PurchaseState.PURCHASED &&
                            purchase.products.contains(ProductIds.QUILL_PRO)
                }

                if (existingSub != null) {
                    // The device already owns it! Abort the launch and show our custom error.
                    _events.emit(
                        BillingEvent.Error(
                            "This Google Play account is already subscribed. To subscribe on this Quill profile, please switch to a different Google account in the Play Store app."
                        )
                    )
                    return@launch // Stop execution here
                }
            }

            // 2. If no existing sub, switch to the Main thread and launch the Google Play UI
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                val product = _subProducts.value.find { it.productId == ProductIds.QUILL_PRO }

                if (product == null) {
                    _events.emit(BillingEvent.Error("Product details not loaded. Please try again."))
                    return@withContext
                }

                val offerToken = product.subscriptionOfferDetails
                    ?.firstOrNull { it.basePlanId == basePlanId }
                    ?.offerToken

                if (offerToken == null) {
                    _events.emit(BillingEvent.Error("Selected plan unavailable."))
                    return@withContext
                }

                val flowParams = BillingFlowParams.newBuilder()
                    .setObfuscatedAccountId(userId)
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(product)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    fun launchTipFlow(activity: Activity, productId: String) {
        scope.launch {
            val product = _tipProducts.value.find { it.productId == productId }
            if (product == null) {
                _events.emit(BillingEvent.Error("Product not available. Please try again."))
                return@launch
            }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(product)
                            .build()
                    )
                )
                .build()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                billingClient.launchBillingFlow(activity, params)
            }
        }
    }

    // ── 4. restorePurchases ───────────────────────────────────────────────────

    /**
     * Checks Google Play for active subscriptions belonging to this userId.
     * Returns true if a valid Pro subscription was found and applied.
     */
    suspend fun restorePurchases(userId: String): Boolean? {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val (result, purchases) = billingClient.queryPurchasesAsync(params)

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            val validPurchase = purchases.find { purchase ->
                val isPurchased = purchase.purchaseState == PurchaseState.PURCHASED
                val isProProduct = purchase.products.contains(ProductIds.QUILL_PRO)
                // Verify the receipt "tag" matches the current Quill account
                val belongsToUser = purchase.accountIdentifiers?.obfuscatedAccountId.let { id ->
                    id == null || id == userId  // null = old purchase without account tagging, still accept it
                }

                isPurchased && isProProduct && belongsToUser
            }

            val hasPro = validPurchase != null
            _isPro.value = hasPro

            if (hasPro) {
                // If it's valid but not acknowledged yet (edge case), acknowledge it
                handleSubscription(validPurchase)
            }

            return hasPro
        }
        // Return null instead of false on network errors!
        Timber.w("Failed to query purchases. Code: ${result.responseCode}")
        return null
    }

    // ── 5. handlePurchase (internal) ──────────────────────────────────────────

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != PurchaseState.PURCHASED) return
        when {
            purchase.products.any { it in ProductIds.subs } -> handleSubscription(purchase)
            purchase.products.any { it in ProductIds.tips } -> handleTip(purchase)
        }
    }

    private suspend fun handleSubscription(purchase: Purchase) {
        // Double-check user ownership before granting access
        val belongsToUser = purchase.accountIdentifiers?.obfuscatedAccountId == currentUserId
        if (!belongsToUser) {
            Timber.w("Subscription owned by different account. Access denied.")
            return
        }

        if (purchase.isAcknowledged) {
            _isPro.value = true
            return
        }

        val ackResult = billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        )

        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isPro.value = true
            _events.emit(BillingEvent.SubscriptionActivated)
        }
    }

    private suspend fun handleTip(purchase: Purchase) {
        val (result, _) = billingClient.consumePurchase(
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        )
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _events.emit(BillingEvent.TipThankYou)
        }
    }
}