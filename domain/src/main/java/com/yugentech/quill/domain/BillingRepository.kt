package com.yugentech.quill.domain

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    /**
     * Whether the signed-in account has Pro. Google Play is the only source: this is its answer,
     * or the last one it gave for this account (remembered on this phone) until it has answered.
     */
    val isPro: StateFlow<Boolean>
    val subProducts: StateFlow<List<ProductDetails>>
    val tipProducts: StateFlow<List<ProductDetails>>
    val billingEvents: Flow<BillingEvent>
    fun startConnection()

    /** The signed-in account's ID, or null. [isPro] is that account's by the time this returns. */
    fun setUserId(userId: String?)
    fun launchSubscriptionFlow(activity: Activity, basePlanId: String, userId: String)
    fun launchTipFlow(activity: Activity, productId: String)

    /** Asks Play again what's owned. Returns whether the account has Pro, or null if Play couldn't be asked. */
    suspend fun refreshPurchases(): Boolean?
    suspend fun restorePurchases(userId: String): Boolean?
}
