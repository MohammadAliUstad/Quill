package com.yugentech.quill.quota.manager

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

                    billingRepository.setUserId(user.uid)
                    billingRepository.startConnection()

                    applicationScope.launch {
                        delay(2000)
                        val verifiedStatus = billingRepository.restorePurchases(user.uid)

                        if (verifiedStatus != null) {
                            userRepository.updateProStatus(user.uid, verifiedStatus)
                            Timber.d("Global Sync: Pro status verified as $verifiedStatus")
                        }
                    }

                    userRepository.getUserFlow(user.uid).collectLatest { userData ->
                        val isPro = userData?.isPro == true

                        quotaRepository.onProStatusChanged(user.uid, isPro)
                    }
                }
            }
        }
    }
}