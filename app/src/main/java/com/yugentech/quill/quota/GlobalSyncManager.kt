package com.yugentech.quill.quota

import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.domain.QuotaRepository
import com.yugentech.quill.user.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class GlobalSyncManager(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val quotaRepository: QuotaRepository,
    private val billingRepository: BillingRepository
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startObserving()
    }

    private fun startObserving() {
        applicationScope.launch {
            Timber.d("Starting Global Sync for Billing & Quota...")

            authRepository.authState.collectLatest { user ->
                if (user != null) {

                    // 1. Boot up the Google Play connection immediately
                    billingRepository.setUserId(user.uid)
                    billingRepository.startConnection()

                    // 2. Perform the Silent Reality Check in the background
                    applicationScope.launch {
                        delay(2000) // Give the BillingClient 2 seconds to connect
                        val verifiedStatus = billingRepository.restorePurchases(user.uid)

                        // If Play Store confirms a status, force the DB to match reality
                        if (verifiedStatus != null) {
                            userRepository.updateProStatus(user.uid, verifiedStatus)
                            Timber.d("Global Sync: Pro status verified as $verifiedStatus")
                        }
                    }

                    // 3. Keep Quota and UI perfectly in sync with the Database
                    userRepository.getUserFlow(user.uid).collectLatest { userData ->
                        val isPro = userData?.isPro == true

                        // This perfectly syncs the cloud limit AND loads the quota into Room!
                        quotaRepository.onProStatusChanged(user.uid, isPro)
                    }
                }
            }
        }
    }
}