package com.yugentech.quill.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.domain.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class BillingRepositoryImpl(
    private val service: BillingClientService
) : BillingRepository {

    override val isPro: StateFlow<Boolean> = service.isPro
    override val subProducts: StateFlow<List<ProductDetails>> = service.subProducts
    override val tipProducts: StateFlow<List<ProductDetails>> = service.tipProducts
    override val billingEvents: Flow<BillingEvent> = service.events

    override fun startConnection() = service.connect()

    override fun setUserId(userId: String?) {
        service.setCurrentUser(userId)
    }

    // 1. Accept userId and pass it to the service
    override fun launchSubscriptionFlow(activity: Activity, basePlanId: String, userId: String) =
        service.launchSubscriptionFlow(activity, basePlanId, userId)

    override fun launchTipFlow(activity: Activity, productId: String) =
        service.launchTipFlow(activity, productId)

    // UPDATED: Explicitly return Boolean? to match the interface and service
    override suspend fun restorePurchases(userId: String): Boolean? =
        service.restorePurchases(userId)
}