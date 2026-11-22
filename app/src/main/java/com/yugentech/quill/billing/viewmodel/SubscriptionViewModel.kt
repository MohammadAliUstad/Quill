package com.yugentech.quill.billing.viewmodel

import android.os.SystemClock
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.R
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.localization.AppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How a tap on Restore ended, when there's something to say: shown in a dialog. */
enum class RestoreResult {
    /** Play answered, and this account has no active subscription. */
    NotFound,
    /** Couldn't reach Google Play. */
    ConnectionFailed,
    /** A guest: Pro belongs to an account. */
    NeedsAccount,
    /** Nobody signed in. */
    NotSignedIn
}

// How long Restore shows its thinking dots at the least, from the tap
private const val MIN_RESTORING_MILLIS = 2_500L

/**
 * After the dots go, how long before the result shows: long enough for Restore to slide back
 * into its button (the stagger plus the slide-in in SubscribeBottomBar) and settle.
 */
private const val RESULT_AFTER_MILLIS = 700L

/**
 * The subscription screen. It only starts purchases and shows how they ended: saving Pro to the
 * account's profile is GlobalSyncManager's job, so it happens whichever screen is open.
 */
class SubscriptionViewModel(
    private val billingRepository: BillingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingRepository.isPro

    val subProducts = billingRepository.subProducts

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    // A restore that found Pro needs no dialog: the screen turns into the subscribed one
    private val _restoreResult = MutableStateFlow<RestoreResult?>(null)
    val restoreResult: StateFlow<RestoreResult?> = _restoreResult.asStateFlow()

    fun dismissRestoreResult() {
        _restoreResult.value = null
    }

    private val _events = MutableSharedFlow<BillingEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            billingRepository.billingEvents.collect { _events.emit(it) }
        }
    }

    fun subscribe(activity: Activity, basePlanId: String) {
        // Pro belongs to an account, so a guest makes one first
        if (authRepository.isGuest) {
            viewModelScope.launch { _events.emit(BillingEvent.Error(AppStrings.get(R.string.guest_subscribe_needs_account))) }
            return
        }
        val userId = authRepository.currentUser

        if (userId != null) {
            billingRepository.launchSubscriptionFlow(activity, basePlanId, userId)
        } else {
            viewModelScope.launch {
                _events.emit(BillingEvent.Error(AppStrings.get(R.string.billing_login_subscribe)))
            }
        }
    }

    fun restorePurchases() {
        if (authRepository.isGuest) {
            _restoreResult.value = RestoreResult.NeedsAccount
            return
        }
        val userId = authRepository.currentUser

        if (userId == null) {
            _restoreResult.value = RestoreResult.NotSignedIn
            return
        }
        if (_isRestoring.value) return

        viewModelScope.launch {
            _isRestoring.value = true
            val startedAt = SystemClock.elapsedRealtime()

            val wasRestored = billingRepository.restorePurchases(userId)

            // The thinking dots stay a moment even when Play answers at once, so the button
            // doesn't flick straight back to Restore
            val shownFor = SystemClock.elapsedRealtime() - startedAt
            delay((MIN_RESTORING_MILLIS - shownFor).coerceAtLeast(0))
            _isRestoring.value = false
            delay(RESULT_AFTER_MILLIS)

            when (wasRestored) {
                true -> _events.emit(BillingEvent.SubscriptionActivated)
                false -> _restoreResult.value = RestoreResult.NotFound
                null -> _restoreResult.value = RestoreResult.ConnectionFailed
            }
        }
    }
}
