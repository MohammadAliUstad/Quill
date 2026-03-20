package com.yugentech.quill.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.domain.BillingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val billingRepository: BillingRepository
) : ViewModel() {

    // Reflects the user's current Pro status from Play Billing
    val isPro: StateFlow<Boolean> = billingRepository.isPro

    // ProductDetails for quill_pro_monthly — contains both base plans with real prices
    val subProducts = billingRepository.subProducts

    // Tracks whether a restore is in progress to show loading state
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    // Re-emits billing events to the screen for snackbar/toast display
    private val _events = MutableSharedFlow<BillingEvent>()
    val events = _events.asSharedFlow()

    init {
        // Forward billing events from the repo into the screen-scoped event flow
        viewModelScope.launch {
            billingRepository.billingEvents.collect { _events.emit(it) }
        }
    }

    // Called when the user taps "Get Started" — basePlanId is "monthly-base" or "yearly-base"
    fun subscribe(activity: Activity, basePlanId: String) {
        billingRepository.launchSubscriptionFlow(activity, basePlanId)
    }

    // Called from the "Restore Purchases" button — queries Play for existing active subs
    fun restorePurchases() {
        viewModelScope.launch {
            _isRestoring.value = true
            billingRepository.restorePurchases()
            _isRestoring.value = false
        }
    }
}