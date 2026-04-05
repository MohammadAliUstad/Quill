package com.yugentech.quill.quota

import com.yugentech.quill.database.dao.QuotaDao
import com.yugentech.quill.database.entity.QuotaEntity
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.QuotaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class QuotaRepositoryImpl(
    authRepository: AuthRepository,
    private val quotaService: QuotaService,
    private val quotaDao: QuotaDao
) : QuotaRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // 1. THE SINGLE SOURCE OF TRUTH: The UI directly observes Room!
    override val remainingQueries: StateFlow<Int> = authRepository.authState
        .map { user -> user?.uid }
        .flatMapLatest { uid ->
            if (uid != null) quotaDao.observeQuota(uid).map { it?.remaining ?: 10 }
            else flowOf(0)
        }
        .stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), 10)

    override val canSendQuery: StateFlow<Boolean> = authRepository.authState
        .map { user -> user?.uid }
        .flatMapLatest { uid ->
            if (uid != null) quotaDao.observeQuota(uid).map { it?.hasQuota ?: true }
            else flowOf(false)
        }
        .stateIn(repositoryScope, SharingStarted.WhileSubscribed(5000), true)


    // 2. THE BACKGROUND SYNC
    override suspend fun loadQuota(userId: String, isPro: Boolean) {
        var networkQuota = quotaService.fetchQuota(userId)

        when {
            networkQuota == null -> {
                quotaService.initQuota(userId, isPro)
                networkQuota = quotaService.fetchQuota(userId)
            }

            // UPDATE 1: Only reset if it is NOT a lifetime (Free) quota
            networkQuota.isExpired && !networkQuota.isLifetime -> {
                quotaService.resetQuota(userId)
                networkQuota = quotaService.fetchQuota(userId)
            }
        }

        if (networkQuota != null) {
            val entity = QuotaEntity(
                userId = userId,
                queriesUsed = networkQuota.queriesUsed,
                queriesLimit = networkQuota.queriesLimit,
                resetAtMillis = networkQuota.resetAt?.toDate()?.time ?: 0L,
                isLifetime = networkQuota.isLifetime
            )
            // Saving to Room automatically triggers the StateFlows above to update the UI!
            quotaDao.saveQuota(entity)
        }
    }

    // 3. USER ACTIONS
    override suspend fun consumeQuery(userId: String): Boolean {
        var currentQuota = quotaDao.getQuota(userId)

        // UPDATE 2: Ensure we only reset expired quotas if they aren't lifetime (Free) accounts
        if (currentQuota?.isExpired == true && currentQuota.isLifetime == false) {
            val newResetTime = System.currentTimeMillis() + 86400000L // 24 hours from right now

            quotaDao.resetUsage(userId, newResetTime) // Update local DB instantly
            quotaService.resetQuota(userId)           // Sync the reset to the cloud

            currentQuota = quotaDao.getQuota(userId)  // Re-fetch the fresh local quota
        }

        // If they still have no quota after the reset check, block the query
        if (currentQuota?.hasQuota == false) return false

        // 1. Instantly update Room (This automatically triggers the UI flows above to update!)
        quotaDao.incrementUsage(userId)

        // 2. Silently sync to Cloud (Fire and forget)
        quotaService.incrementUsage(userId)

        return true
    }

    override suspend fun onProStatusChanged(userId: String, isPro: Boolean) {
        // Step 1: Update the limits and booleans in Firestore
        quotaService.updateLimit(userId, isPro)

        // Step 2: Re-fetch the completely updated Firestore document and cleanly overwrite Room.
        loadQuota(userId, isPro)
    }
}