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
    // No more manual MutableStateFlows. If Room changes, the UI updates instantly.

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


    // 2. THE BACKGROUND SYNC (Called ONCE by GlobalSyncManager on launch)
    override suspend fun loadQuota(userId: String, isPro: Boolean) {
        var networkQuota = quotaService.fetchQuota(userId)

        when {
            networkQuota == null -> {
                quotaService.initQuota(userId, isPro)
                networkQuota = quotaService.fetchQuota(userId)
            }

            networkQuota.isExpired -> {
                quotaService.resetQuota(userId)
                networkQuota = quotaService.fetchQuota(userId)
            }
        }

        if (networkQuota != null) {
            val entity = QuotaEntity(
                userId = userId,
                queriesUsed = networkQuota.queriesUsed,
                queriesLimit = networkQuota.queriesLimit,
                // THE FIX: Safely unwrap the timestamp, or default to 0 if missing
                resetAtMillis = networkQuota.resetAt?.toDate()?.time ?: 0L
            )
            // Saving to Room automatically triggers the StateFlows above to update the UI!
            quotaDao.saveQuota(entity)
        }
    }

    // 3. USER ACTIONS (Write locally immediately, sync to cloud quietly)
    override suspend fun consumeQuery(userId: String): Boolean {
        // We check the DB directly to ensure they have quota
        val currentQuota = quotaDao.getQuota(userId)
        if (currentQuota?.hasQuota == false) return false

        // 1. Instantly update Room (This automatically triggers the UI flows above to update!)
        quotaDao.incrementUsage(userId)

        // 2. Silently sync to Cloud (Fire and forget)
        quotaService.incrementUsage(userId)

        return true
    }

    override suspend fun onProStatusChanged(userId: String, isPro: Boolean) {
        val newLimit = if (isPro) QuotaLimits.PRO else QuotaLimits.FREE
        quotaDao.updateLimit(userId, newLimit) // Updates UI via Room
        quotaService.updateLimit(userId, isPro) // Syncs to Cloud

        loadQuota(userId, isPro)
    }
}