package com.yugentech.quill.quota.manager

import com.yugentech.quill.bookDetails.repository.BookDetailsRepository
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.domain.QuotaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Sets billing and Aira's quota up for whoever is signed in, for as long as the app runs.
 *
 * Google Play alone says who has Pro (see BillingRepository.isPro); nothing here saves it. This
 * tells billing which account is signed in, asks Play what it owns, loads the account's query
 * count, and queues any downloaded book whose index for Aira isn't finished. Every account's
 * books are indexed, Pro or not.
 */
class GlobalSyncManager(
    private val authRepository: AuthRepository,
    private val quotaRepository: QuotaRepository,
    private val billingRepository: BillingRepository,
    private val bookRepository: BookDetailsRepository
) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startObserving()
    }

    private fun startObserving() {
        applicationScope.launch {
            Timber.d("Starting Global Sync for Billing & Quota...")

            // collectLatest: everything below stops when the account changes
            authRepository.authState.collectLatest { user ->
                // A guest has no account for Pro or Aira's quota to belong to: neither is used
                if (user == null || user.isGuest) {
                    billingRepository.setUserId(null)
                    return@collectLatest
                }
                val userId = user.uid

                // From here isPro is this account's: Play's last answer for it, until Play answers again
                billingRepository.setUserId(userId)
                billingRepository.startConnection()

                coroutineScope {
                    launch { billingRepository.refreshPurchases() }
                    launch { quotaRepository.loadQuota(userId) }
                    // Books whose indexing failed or was cut short (the app closed, the phone
                    // restarted) get queued again, so they don't stay unready for Aira
                    launch { bookRepository.indexUnfinishedBooks() }
                }
            }
        }
    }
}
