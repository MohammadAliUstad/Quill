package com.yugentech.quill.billing.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.aira.book.BookRepository
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.user.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val billingRepository: BillingRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    val subProducts = billingRepository.subProducts

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { user ->
                if (user != null) {
                    userRepository.getUserFlow(user.uid).collect { userData ->
                        _isPro.value = userData?.isPro == true
                    }
                } else {
                    _isPro.value = false
                }
            }
        }

        viewModelScope.launch {
            billingRepository.billingEvents.collect { event ->
                if (event is BillingEvent.SubscriptionActivated) {
                    authRepository.currentUser?.let { uid ->
                        userRepository.updateProStatus(uid, true)
                    }
                }
                _events.emit(event)
            }
        }
    }

    fun subscribe(activity: Activity, basePlanId: String) {
        val userId = authRepository.currentUser

        if (userId != null) {
            billingRepository.launchSubscriptionFlow(activity, basePlanId, userId)
        } else {
            viewModelScope.launch {
                _events.emit(BillingEvent.Error("You must be logged in to subscribe."))
            }
        }
    }

    fun restorePurchases() {
        val userId = authRepository.currentUser

        if (userId == null) {
            viewModelScope.launch {
                _events.emit(BillingEvent.Error("You must be logged in to restore purchases."))
            }
            return
        }

        viewModelScope.launch {
            _isRestoring.value = true

            val wasRestored = billingRepository.restorePurchases(userId)

            delay(800)
            _isRestoring.value = false

            when (wasRestored) {
                true -> {
                    userRepository.updateProStatus(userId, true)
                    _events.emit(BillingEvent.SubscriptionActivated)
                }
                false -> {
                    userRepository.updateProStatus(userId, false)
                    _events.emit(BillingEvent.NoSubscriptionFound)
                }
                null -> {
                    _events.emit(BillingEvent.Error("Could not connect to Google Play. Please check your internet connection and try again."))
                }
            }
        }
    }
}