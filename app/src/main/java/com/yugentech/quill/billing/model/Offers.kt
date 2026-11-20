package com.yugentech.quill.billing.model

import com.android.billingclient.api.ProductDetails

/**
 * The offer to buy for [basePlanId]. Play lists only the offers this user can have, so a special
 * offer on the plan (a free trial, say) is taken when there is one, and the plain plan otherwise.
 */
fun ProductDetails.offerFor(basePlanId: String): ProductDetails.SubscriptionOfferDetails? {
    val offers = subscriptionOfferDetails?.filter { it.basePlanId == basePlanId }.orEmpty()
    return offers.firstOrNull { it.offerId != null } ?: offers.firstOrNull()
}

/**
 * The price [basePlanId] renews at: the last pricing phase. Any phases before it are a free trial
 * or an introductory price, which Play shows on its own sheet.
 */
fun ProductDetails.recurringPrice(basePlanId: String): String? =
    offerFor(basePlanId)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
