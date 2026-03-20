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

    // Delegate all flows directly from the service — no transformation needed
    override val isPro: StateFlow<Boolean> = service.isPro
    override val subProducts: StateFlow<List<ProductDetails>> = service.subProducts
    override val tipProducts: StateFlow<List<ProductDetails>> = service.tipProducts
    override val billingEvents: Flow<BillingEvent> = service.events

    // Delegate connection to the service — called once from MainActivity
    override fun startConnection() = service.connect()

    // Delegate subscription launch — basePlanId is "monthly-base" or "yearly-base"
    override fun launchSubscriptionFlow(activity: Activity, basePlanId: String) =
        service.launchSubscriptionFlow(activity, basePlanId)

    // Delegate tip launch — productId is "tip_coffee" or "tip_lunch"
    override fun launchTipFlow(activity: Activity, productId: String) =
        service.launchTipFlow(activity, productId)

    // Delegate restore — called from the Restore Purchases button on SubscriptionScreen
    override suspend fun restorePurchases() = service.restorePurchases()
}