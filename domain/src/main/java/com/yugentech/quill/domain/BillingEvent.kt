package com.yugentech.quill.domain

sealed class BillingEvent {
    data object TipThankYou : BillingEvent()
    data object SubscriptionActivated : BillingEvent()
    data object UserCancelled : BillingEvent()
    data object NoSubscriptionFound : BillingEvent()
    data class Error(val message: String) : BillingEvent()
}