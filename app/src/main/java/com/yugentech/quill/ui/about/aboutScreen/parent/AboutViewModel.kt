package com.yugentech.quill.ui.about.aboutScreen.parent

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.domain.BillingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AboutViewModel(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<BillingEvent>()
    val events = _events.asSharedFlow()

    init {
        // Forward billing events from the repo into the screen-scoped event flow
        viewModelScope.launch {
            billingRepository.billingEvents.collect { _events.emit(it) }
        }
    }

    // Called when user taps "Buy me a Coffee"
    fun buyCoffee(activity: Activity) {
        billingRepository.launchTipFlow(activity, "donation_coffee")
    }

    fun buyLunch(activity: Activity) {
        billingRepository.launchTipFlow(activity, "donation_lunch")
    }
}