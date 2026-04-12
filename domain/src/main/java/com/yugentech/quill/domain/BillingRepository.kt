package com.yugentech.quill.domain

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val isPro: StateFlow<Boolean>
    val subProducts: StateFlow<List<ProductDetails>>
    val tipProducts: StateFlow<List<ProductDetails>>
    val billingEvents: Flow<BillingEvent>
    fun startConnection()
    fun setUserId(userId: String?)
    fun launchSubscriptionFlow(activity: Activity, basePlanId: String, userId: String)
    fun launchTipFlow(activity: Activity, productId: String)
    suspend fun restorePurchases(userId: String): Boolean?
}