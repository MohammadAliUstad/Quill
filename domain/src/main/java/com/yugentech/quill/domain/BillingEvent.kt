package com.yugentech.quill.domain

sealed class BillingEvent {
    data object TipThankYou : BillingEvent()
    data object SubscriptionActivated : BillingEvent()
    data object UserCancelled : BillingEvent()
    data object NoSubscriptionFound : BillingEvent()

    /** The payment was started but isn't complete yet (cash, a slow card): nothing is unlocked until it is. */
    data object PurchasePending : BillingEvent()
    data class Error(val message: String) : BillingEvent()
}
