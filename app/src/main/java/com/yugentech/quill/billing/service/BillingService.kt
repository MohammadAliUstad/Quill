package com.yugentech.quill.billing.service

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
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
import com.yugentech.quill.R
import com.yugentech.quill.billing.model.ProductIds
import com.yugentech.quill.billing.model.offerFor
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.localization.AppStrings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import android.os.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume

private const val CONNECTION_TIMEOUT_MS = 15_000L
private const val ACK_ATTEMPTS = 3

// A refresh this recent answers the next one (they come in threes at launch)
private const val RECENT_REFRESH_MILLIS = 5_000L

/**
 * Google Play billing: the Pro subscription and the tips.
 *
 * Play is the only authority on who has Pro: nothing about it is kept on the account's profile
 * or in Firestore. [isPro] is Play's answer for the signed-in account. Until Play has answered
 * (the first moment after launch, or when it can't be reached) it's the last answer Play gave for
 * that account, remembered on this phone only.
 *
 * A purchase made in the app carries the buyer's Quill account ID. One made outside it (a promo
 * code redeemed in the Play Store, a resubscription from Play's subscriptions page) carries none,
 * and goes to whoever is signed in, as Play recommends. One carrying another account's ID is
 * never used.
 *
 * Every purchase is acknowledged (a tip: consumed), or Play refunds it after three days. If that
 * fails, it's tried again at the next [refreshPurchases].
 */
class BillingService(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _subProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val subProducts = _subProducts.asStateFlow()

    private val _tipProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val tipProducts = _tipProducts.asStateFlow()

    // Buffered so reporting an event never holds up purchase handling
    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    // Play's last answer and the account it was for, so Pro is right from the first frame
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val userLock = Any()

    @Volatile
    private var currentUserId: String? = null

    // One purchase is handled at a time, so the same one is never acknowledged or consumed twice
    private val purchaseMutex = Mutex()

    // Launch asks Play from three places at once (the connection coming up, the account being
    // set, the screen resuming): they share one answer. A refresh waits for one already running,
    // and one that finished moments ago, for the same account, answers the next caller.
    private val refreshGate = Mutex()
    private var lastRefresh: CachedRefresh? = null

    private class CachedRefresh(val userId: String, val hasPro: Boolean, val atMillis: Long)

    // Which flow was opened last: Play's "already owned" doesn't say which product it means
    @Volatile
    private var lastFlowWasTip = false

    /**
     * The signed-in account's ID, or null for a guest or no one. [isPro] becomes Play's last
     * answer for that account if this phone remembers one, and false otherwise, before this returns.
     */
    fun setCurrentUser(userId: String?) {
        synchronized(userLock) {
            if (userId == currentUserId) return
            currentUserId = userId
            _isPro.value = userId != null &&
                    prefs.getString(KEY_USER_ID, null) == userId &&
                    prefs.getBoolean(KEY_IS_PRO, false)
        }
        Timber.d("Billing user changed")
    }

    // Only for the account Play was asked about: it may have changed while Play was answering
    private fun setVerifiedPro(userId: String, isPro: Boolean) {
        synchronized(userLock) {
            if (currentUserId != userId) return
            _isPro.value = isPro
            prefs.edit {
                putString(KEY_USER_ID, userId)
                putBoolean(KEY_IS_PRO, isPro)
            }
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                scope.launch {
                    purchases.orEmpty().forEach { purchase ->
                        purchaseMutex.withLock { handlePurchase(purchase, announce = true) }
                    }
                }
            }

            BillingResponseCode.USER_CANCELED -> {
                _events.tryEmit(BillingEvent.UserCancelled)
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch { handleAlreadyOwned() }
            }

            else -> {
                Timber.e("Purchase error [${result.responseCode}]: ${result.debugMessage}")
                _events.tryEmit(BillingEvent.Error(messageFor(result.responseCode)))
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
                if (result.responseCode == BillingResponseCode.OK) {
                    Timber.d("BillingClient connected")
                    attempt.complete(true)
                    scope.launch {
                        querySubProducts()
                        queryTipProducts()
                        refreshPurchases()
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
                if (result.responseCode != BillingResponseCode.OK) {
                    Timber.e("Product query ($type) failed [${result.responseCode}]: ${result.debugMessage}")
                    if (cont.isActive) cont.resume(null)
                    return@queryProductDetailsAsync
                }

                queryResult.unfetchedProductList.forEach { unfetched ->
                    Timber.w("Product not fetched ($type): ${unfetched.productId} status=${unfetched.statusCode}")
                }

                if (cont.isActive) cont.resume(queryResult.productDetailsList)
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
                _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_unreachable)))
                return@launch
            }
            if (userId != currentUserId) {
                // The account changed under the screen; buying now would tie Pro to the wrong one
                _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_error_generic)))
                return@launch
            }

            // The Play account may already hold Pro: bought on another phone, or a promo code
            // redeemed in the Play Store. Play would refuse a second purchase, so settle it here.
            val (result, purchases) = billingClient.queryPurchasesAsync(subsQuery())
            if (result.responseCode == BillingResponseCode.OK) {
                val existing = purchases.find { it.products.contains(ProductIds.QUILL_PRO) }
                if (existing != null) {
                    if (existing.belongsTo(userId)) {
                        // This account's own: unlock it (or say it's still pending), don't sell it again
                        purchaseMutex.withLock { handleSubscription(existing, announce = true) }
                    } else {
                        _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_already_subscribed)))
                    }
                    return@launch
                }
            }

            val product = findSubProduct()
            if (product == null) {
                _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_details_missing)))
                return@launch
            }

            val offerToken = product.offerFor(basePlanId)?.offerToken
            if (offerToken == null) {
                Timber.e("No offer for base plan $basePlanId; available: ${product.subscriptionOfferDetails?.map { it.basePlanId }}")
                _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_plan_unavailable)))
                return@launch
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

            lastFlowWasTip = false
            launchFlow(activity, flowParams)
        }
    }

    fun launchTipFlow(activity: Activity, productId: String) {
        scope.launch {
            if (!ensureConnected()) {
                _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_unreachable)))
                return@launch
            }
            val product = findTipProduct(productId)
            if (product == null) {
                _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_product_unavailable)))
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

            lastFlowWasTip = true
            launchFlow(activity, params)
        }
    }

    private suspend fun launchFlow(activity: Activity, params: BillingFlowParams) {
        val result: BillingResult = withContext(Dispatchers.Main) {
            if (activity.isFinishing || activity.isDestroyed) null
            else billingClient.launchBillingFlow(activity, params)
        } ?: return

        // Once Play's sheet opens, the outcome comes through the purchases listener. These are
        // the cases where it never opened, which the listener doesn't hear about.
        when (result.responseCode) {
            BillingResponseCode.OK,
            BillingResponseCode.USER_CANCELED,
            BillingResponseCode.ITEM_ALREADY_OWNED -> Unit

            else -> {
                Timber.e("Couldn't open the purchase sheet [${result.responseCode}]: ${result.debugMessage}")
                _events.emit(BillingEvent.Error(messageFor(result.responseCode)))
            }
        }
    }

    /**
     * Asks Play what this Play account owns and settles it: Pro is set for the signed-in account
     * (and the purchase acknowledged if it wasn't), and any tip left unfinished is consumed.
     * Returns whether the account has Pro, or null when Play couldn't be asked or no one is signed in.
     */
    suspend fun refreshPurchases(): Boolean? = refresh(force = false)

    /** [refreshPurchases] for the Restore button; null too if [userId] is no longer the signed-in account. */
    suspend fun restorePurchases(userId: String): Boolean? {
        if (userId != currentUserId) return null
        // The user asked: always Play's answer now, never one from moments ago. The Restore button
        // announces what it found itself, so nothing is announced here too.
        return refresh(force = true, callerAnnounces = true)
    }

    /**
     * Asks Play what the account owns. A purchase acknowledged only now (the app closed before it
     * was) is announced as activated, unless [callerAnnounces]: the caller tells the user what
     * came of it, and Pro mustn't be announced twice.
     */
    private suspend fun refresh(force: Boolean, callerAnnounces: Boolean = false): Boolean? = refreshGate.withLock {
        val userId = currentUserId
        val recent = lastRefresh
        if (!force && recent != null && recent.userId == userId &&
            SystemClock.elapsedRealtime() - recent.atMillis < RECENT_REFRESH_MILLIS
        ) {
            return@withLock recent.hasPro
        }
        if (!ensureConnected()) return@withLock null

        val hasPro = purchaseMutex.withLock {
            finishTips()
            refreshSubscription(announceLate = !callerAnnounces)
        }
        // Only a real answer, for the account still signed in, is worth reusing
        lastRefresh = if (hasPro != null && userId != null && userId == currentUserId) {
            CachedRefresh(userId, hasPro, SystemClock.elapsedRealtime())
        } else {
            null
        }
        hasPro
    }

    // Callers hold purchaseMutex
    private suspend fun refreshSubscription(announceLate: Boolean): Boolean? {
        val userId = currentUserId ?: return null
        val (result, purchases) = billingClient.queryPurchasesAsync(subsQuery())
        if (result.responseCode != BillingResponseCode.OK) {
            Timber.w("Failed to query subscriptions [${result.responseCode}]: ${result.debugMessage}")
            return null
        }
        if (userId != currentUserId) return null

        val purchase = purchases.find {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    it.products.contains(ProductIds.QUILL_PRO) &&
                    it.belongsTo(userId)
        }
        if (purchase == null) {
            setVerifiedPro(userId, false)
            return false
        }
        return handleSubscription(purchase, announce = false, announceLate = announceLate)
    }

    // A tip bought but never consumed (the app closed first, or a slow payment came through
    // later) can't be bought again and is refunded after three days. Callers hold purchaseMutex.
    private suspend fun finishTips() {
        val (result, purchases) = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        if (result.responseCode != BillingResponseCode.OK) {
            Timber.w("Failed to query tips [${result.responseCode}]: ${result.debugMessage}")
            return
        }
        purchases
            .filter { purchase -> purchase.products.any { it in ProductIds.tips } }
            .forEach { handleTip(it, announce = false) }
    }

    // Play says the product is already owned: something bought earlier was never finished here
    private suspend fun handleAlreadyOwned() {
        val wasTip = lastFlowWasTip
        // Play has just said something's owned: ask it now, not a cached answer. Pro is announced
        // just below, once; after a tip only the retry is, so a late subscription announces itself.
        val hasPro = refresh(force = true, callerAnnounces = !wasTip)
        when {
            // finishTips() has just consumed the earlier tip, so the next try goes through
            wasTip -> _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_tip_retry)))
            hasPro == true -> _events.emit(BillingEvent.SubscriptionActivated)
            else -> _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_already_subscribed_new)))
        }
    }

    // Callers hold purchaseMutex
    private suspend fun handlePurchase(purchase: Purchase, announce: Boolean) {
        when {
            purchase.products.any { it in ProductIds.subs } -> handleSubscription(purchase, announce)
            purchase.products.any { it in ProductIds.tips } -> handleTip(purchase, announce)
            else -> Timber.w("Purchase of an unknown product ignored")
        }
    }

    /**
     * Gives the signed-in account Pro for [purchase] if it's theirs and paid for, and acknowledges
     * it. Returns whether they have Pro from it. [announce] is for a purchase the user is waiting
     * on: they're told how it ended. [announceLate] announces one that's only acknowledged now,
     * which no one was waiting on. Callers hold [purchaseMutex].
     */
    private suspend fun handleSubscription(purchase: Purchase, announce: Boolean, announceLate: Boolean = true): Boolean {
        // No account to give it to (a guest, or signed out): it's picked up at the next sign-in
        val userId = currentUserId ?: return false

        if (!purchase.belongsTo(userId)) {
            Timber.w("Subscription belongs to another Quill account; not used")
            if (announce) _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_already_subscribed)))
            return false
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> Unit
            Purchase.PurchaseState.PENDING -> {
                if (announce) _events.emit(BillingEvent.PurchasePending)
                return false
            }
            else -> return false
        }

        // Paid for, so Pro is theirs now; the acknowledgement is bookkeeping with Play
        setVerifiedPro(userId, true)

        var newlyAcknowledged = false
        if (!purchase.isAcknowledged) {
            newlyAcknowledged = acknowledge(purchase.purchaseToken)
            if (!newlyAcknowledged) Timber.e("Subscription not acknowledged; trying again at the next refresh")
        }

        if (announce || (newlyAcknowledged && announceLate)) _events.emit(BillingEvent.SubscriptionActivated)
        return true
    }

    private suspend fun acknowledge(purchaseToken: String): Boolean {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        for (attempt in 1..ACK_ATTEMPTS) {
            val result = billingClient.acknowledgePurchase(params)
            when (result.responseCode) {
                BillingResponseCode.OK -> return true

                BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingResponseCode.SERVICE_DISCONNECTED,
                BillingResponseCode.NETWORK_ERROR,
                BillingResponseCode.ERROR -> {
                    Timber.w("Acknowledge attempt $attempt failed [${result.responseCode}]: ${result.debugMessage}")
                    if (attempt < ACK_ATTEMPTS) {
                        delay(1000L * attempt)
                        ensureConnected()
                    }
                }

                else -> {
                    Timber.e("Acknowledge refused [${result.responseCode}]: ${result.debugMessage}")
                    return false
                }
            }
        }
        return false
    }

    // Callers hold purchaseMutex
    private suspend fun handleTip(purchase: Purchase, announce: Boolean) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> Unit
            Purchase.PurchaseState.PENDING -> {
                if (announce) _events.emit(BillingEvent.PurchasePending)
                return
            }
            else -> return
        }

        val (result, _) = billingClient.consumePurchase(
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        )
        if (result.responseCode != BillingResponseCode.OK) {
            // finishTips() consumes it at the next refresh
            Timber.e("Tip not consumed [${result.responseCode}]: ${result.debugMessage}")
        }
        // The payment went through either way
        if (announce) _events.emit(BillingEvent.TipThankYou)
    }

    // A purchase made outside the app carries no account ID and goes to whoever is signed in
    private fun Purchase.belongsTo(userId: String): Boolean {
        val owner = accountIdentifiers?.obfuscatedAccountId
        return owner.isNullOrEmpty() || owner == userId
    }

    private fun subsQuery() = QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.SUBS)
        .build()

    // Play's own debug messages are for developers, so the user gets one of ours
    private fun messageFor(responseCode: Int): String = AppStrings.get(
        when (responseCode) {
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.SERVICE_DISCONNECTED,
            BillingResponseCode.NETWORK_ERROR,
            BillingResponseCode.BILLING_UNAVAILABLE -> R.string.billing_unreachable

            BillingResponseCode.ITEM_UNAVAILABLE -> R.string.billing_product_unavailable

            else -> R.string.billing_error_generic
        }
    )

    private companion object {
        const val PREFS = "billing"
        const val KEY_USER_ID = "last_known_user_id"
        const val KEY_IS_PRO = "last_known_is_pro"
    }
}
