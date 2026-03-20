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

    // Dedicated scope — not tied to any ViewModel or screen lifecycle
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cached ProductDetails so launch flows can find them instantly
    private val _subProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val subProducts = _subProducts.asStateFlow()

    private val _tipProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val tipProducts = _tipProducts.asStateFlow()

    // One-shot events: purchase results, errors, thank-you messages
    private val _events = MutableSharedFlow<BillingEvent>()
    val events = _events.asSharedFlow()

    // Emits true when an active Pro subscription is confirmed
    private val _isPro = MutableStateFlow(false)
    val isPro = _isPro.asStateFlow()

    // Single listener that handles results for both tips and subscriptions
    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                purchases?.forEach { scope.launch { handlePurchase(it) } }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                scope.launch { _events.emit(BillingEvent.UserCancelled) }

            else -> {
                Timber.e("Purchase error [${result.responseCode}]: ${result.debugMessage}")
                scope.launch { _events.emit(BillingEvent.Error(result.debugMessage)) }
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            // Required in Billing v7+ — enables one-time product pending states
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── 1. connect ────────────────────────────────────────────────────────────

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingClient connected")
                    scope.launch {
                        // Load product details and restore existing purchases in parallel
                        querySubProducts()
                        queryTipProducts()
                        restorePurchases()
                    }
                } else {
                    Timber.e("BillingClient setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // BillingClient reconnects automatically on the next launchBillingFlow call
                Timber.w("BillingClient disconnected")
            }
        })
    }

    // ── 2. querySubProducts ───────────────────────────────────────────────────

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
            Timber.d("Sub products loaded: ${products?.size}")
        } else {
            Timber.e("Failed to load sub products: ${result.debugMessage}")
        }
    }

    // ── 3. queryTipProducts ───────────────────────────────────────────────────

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
            Timber.d("Tip products loaded: ${products?.size}")
        } else {
            Timber.e("Failed to load tip products: ${result.debugMessage}")
        }
    }

    // ── 4. launchTipFlow ──────────────────────────────────────────────────────

    fun launchTipFlow(activity: Activity, productId: String) {
        val product = _tipProducts.value.find { it.productId == productId }

        if (product == null) {
            scope.launch { _events.emit(BillingEvent.Error("Product not loaded. Check your connection.")) }
            return
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

        billingClient.launchBillingFlow(activity, params)
    }

    // ── 5. launchSubscriptionFlow ─────────────────────────────────────────────

    fun launchSubscriptionFlow(activity: Activity, basePlanId: String) {
        val product = _subProducts.value.find { it.productId == ProductIds.QUILL_PRO }

        if (product == null) {
            scope.launch { _events.emit(BillingEvent.Error("Product not loaded. Check your connection.")) }
            return
        }

        // offerToken identifies which base plan (monthly vs yearly) to purchase
        val offerToken = product.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == basePlanId }
            ?.offerToken

        if (offerToken == null) {
            scope.launch { _events.emit(BillingEvent.Error("Selected plan unavailable.")) }
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, params)
    }

    // ── 6. restorePurchases ───────────────────────────────────────────────────

    suspend fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val (result, purchases) = billingClient.queryPurchasesAsync(params)

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            // Check if any active sub purchase matches our Pro product
            val isPro = purchases.any { purchase ->
                purchase.purchaseState == PurchaseState.PURCHASED &&
                        purchase.products.contains(ProductIds.QUILL_PRO)
            }
            _isPro.value = isPro
            Timber.d("Restore purchases complete: isPro=$isPro")
        } else {
            Timber.e("Failed to restore purchases: ${result.debugMessage}")
        }
    }

    // ── handlePurchase (internal) ─────────────────────────────────────────────

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != PurchaseState.PURCHASED) return

        when {
            // Subscription: acknowledge to prevent auto-refund, then mark user as Pro
            purchase.products.any { it in ProductIds.subs } -> handleSubscription(purchase)

            // Tip: consume immediately so it can be purchased again
            purchase.products.any { it in ProductIds.tips } -> handleTip(purchase)
        }
    }

    private suspend fun handleSubscription(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            // Already acknowledged (e.g. on restore) — just update pro state
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
            Timber.d("Subscription acknowledged")
        } else {
            Timber.e("Acknowledgement failed: ${ackResult.debugMessage}")
        }
    }

    private suspend fun handleTip(purchase: Purchase) {
        val (result, _) = billingClient.consumePurchase(
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        )

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _events.emit(BillingEvent.TipThankYou)
            Timber.d("Tip consumed")
        } else {
            Timber.e("Tip consumption failed: ${result.debugMessage}")
        }
    }
}