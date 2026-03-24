package com.yugentech.quill.domain

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {

    // Emits true when the user has an active Pro subscription
    val isPro: StateFlow<Boolean>

    // Loaded ProductDetails for quill_pro_monthly (contains both base plans)
    val subProducts: StateFlow<List<ProductDetails>>

    // Loaded ProductDetails for tip_coffee and tip_lunch
    val tipProducts: StateFlow<List<ProductDetails>>

    // One-shot UI events: success, error, thank-you, cancelled
    val billingEvents: Flow<BillingEvent>

    // Called once on app start to connect to Play Billing
    fun startConnection()

    fun setUserId(userId: String?)

    // Opens the Play sheet for monthly-base or yearly-base
    fun launchSubscriptionFlow(activity: Activity, basePlanId: String, userId: String)

    // Opens the Play sheet for tip_coffee or tip_lunch
    fun launchTipFlow(activity: Activity, productId: String)

    // UPDATED: Now returns Boolean? to handle network/offline errors safely
    suspend fun restorePurchases(userId: String): Boolean?
}