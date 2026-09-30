package com.yugentech.quill.billing.service

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
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryPurchasesAsync
import com.yugentech.quill.billing.model.ProductIds
import com.yugentech.quill.domain.BillingEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import timber.log.Timber

private const val CONNECTION_TIMEOUT_MS = 15_000L

class BillingService(context: Context) {

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
        // No enableAutoServiceReconnection(): any API call made while the initial connection is
        // still in flight makes it retry startConnection(), which Play rejects with "already in
        // the process of connecting" -- and the whole client ends up disconnected. Reconnection
        // is handled by ensureConnected() below instead.
        .build()

    // The single in-flight connection attempt, shared by every caller that needs Play.
    private var pendingConnection: CompletableDeferred<Boolean>? = null

    fun connect() {
        scope.launch {
            if (ensureConnected()) {
                if (_subProducts.value.isEmpty()) querySubProducts()
                if (_tipProducts.value.isEmpty()) queryTipProducts()
            }
        }
    }

    // Suspends until the client is connected (or the attempt fails/times out). Concurrent callers
    // wait on the same attempt instead of each calling startConnection().
    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true

        val attempt = synchronized(this) {
            pendingConnection?.takeIf { it.isActive }
                ?: CompletableDeferred<Boolean>().also {
                    pendingConnection = it
                    startConnection(it)
                }
        }

        return withTimeoutOrNull(CONNECTION_TIMEOUT_MS) { attempt.await() } ?: run {
            Timber.e("BillingClient connection timed out")
            false
        }
    }

    private fun startConnection(attempt: CompletableDeferred<Boolean>) {
        Timber.d("BillingClient connecting")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingClient connected")
                    attempt.complete(true)
                    scope.launch {
                        querySubProducts()
                        queryTipProducts()

                        currentUserId?.let { restorePurchases(it) }
                    }
                } else {
                    Timber.e("BillingClient setup failed [${result.responseCode}]: ${result.debugMessage}")
                    attempt.complete(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                // The next ensureConnected() call starts a fresh attempt.
                Timber.w("BillingClient disconnected")
                attempt.complete(false)
            }
        })
    }

    private suspend fun querySubProducts() {
        queryProducts(ProductIds.subs, BillingClient.ProductType.SUBS)?.let { _subProducts.value = it }
    }

    private suspend fun queryTipProducts() {
        queryProducts(ProductIds.tips, BillingClient.ProductType.INAPP)?.let { _tipProducts.value = it }
    }

    // Uses the callback API rather than the ktx extension because only the callback exposes
    // unfetchedProductList -- the one place Play says *why* a product ID came back empty.
    private suspend fun queryProducts(ids: List<String>, type: String): List<ProductDetails>? {
        if (!ensureConnected()) return null

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ids.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(type)
                        .build()
                }
            )
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { result, queryResult ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Timber.e("Product query ($type) failed [${result.responseCode}]: ${result.debugMessage}")
                    cont.resume(null)
                    return@queryProductDetailsAsync
                }

                queryResult.unfetchedProductList.forEach { unfetched ->
                    Timber.w("Product not fetched ($type): ${unfetched.productId} status=${unfetched.statusCode}")
                }

                cont.resume(queryResult.productDetailsList)
            }
        }
    }

    // Products may be missing if the initial query failed or never ran. Try once more on demand
    // instead of failing the purchase outright.
    private suspend fun findSubProduct(): ProductDetails? {
        _subProducts.value.find { it.productId == ProductIds.QUILL_PRO }?.let { return it }
        Timber.w("Subscription product missing on purchase; re-querying")
        querySubProducts()
        return _subProducts.value.find { it.productId == ProductIds.QUILL_PRO }
    }

    private suspend fun findTipProduct(productId: String): ProductDetails? {
        _tipProducts.value.find { it.productId == productId }?.let { return it }
        Timber.w("Tip product $productId missing on purchase; re-querying")
        queryTipProducts()
        return _tipProducts.value.find { it.productId == productId }
    }

    fun launchSubscriptionFlow(activity: Activity, basePlanId: String, userId: String) {
        scope.launch {
            if (!ensureConnected()) {
                _events.emit(BillingEvent.Error("Couldn't reach Google Play. Please try again."))
                return@launch
            }

            val queryParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            val (result, purchases) = billingClient.queryPurchasesAsync(queryParams)

            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val existingSub = purchases.find { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.products.contains(ProductIds.QUILL_PRO)
                }

                if (existingSub != null) {
                    _events.emit(
                        BillingEvent.Error(
                            "This Google Play account is already subscribed. To subscribe on this Quill profile, please switch to a different Google account in the Play Store app."
                        )
                    )
                    return@launch
                }
            }

            val product = findSubProduct()

            if (product == null) {
                _events.emit(BillingEvent.Error("Product details not loaded. Please try again."))
                return@launch
            }

            val offerToken = product.subscriptionOfferDetails
                ?.firstOrNull { it.basePlanId == basePlanId }
                ?.offerToken

            if (offerToken == null) {
                Timber.e("No offer for base plan $basePlanId; available: ${product.subscriptionOfferDetails?.map { it.basePlanId }}")
                _events.emit(BillingEvent.Error("Selected plan unavailable."))
                return@launch
            }

            withContext(Dispatchers.Main) {
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
            val product = findTipProduct(productId)
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
            withContext(Dispatchers.Main) {
                billingClient.launchBillingFlow(activity, params)
            }
        }
    }

    suspend fun restorePurchases(userId: String): Boolean? {
        if (!ensureConnected()) return null

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val (result, purchases) = billingClient.queryPurchasesAsync(params)

        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            val validPurchase = purchases.find { purchase ->
                val isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                val isProProduct = purchase.products.contains(ProductIds.QUILL_PRO)
                val belongsToUser = purchase.accountIdentifiers?.obfuscatedAccountId.let { id ->
                    id == null || id == userId
                }

                isPurchased && isProProduct && belongsToUser
            }

            val hasPro = validPurchase != null
            _isPro.value = hasPro

            if (hasPro) {
                handleSubscription(validPurchase)
            }

            return hasPro
        }
        Timber.w("Failed to query purchases. Code: ${result.responseCode}")
        return null
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        when {
            purchase.products.any { it in ProductIds.subs } -> handleSubscription(purchase)
            purchase.products.any { it in ProductIds.tips } -> handleTip(purchase)
        }
    }

    private suspend fun handleSubscription(purchase: Purchase) {
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