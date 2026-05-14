package com.yugentech.quill.quota.repository

import com.yugentech.quill.database.dao.QuotaDao
import com.yugentech.quill.database.entity.QuotaEntity
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.QuotaRepository
import com.yugentech.quill.quota.service.QuotaService
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

    override val remainingQueries: StateFlow<Int> = authRepository.authState
        .map { user -> user?.uid }
        .flatMapLatest { uid ->
            if (uid != null) quotaDao.observeQuota(uid).map { it?.remaining ?: 10 }
            else flowOf(0)
        }
        .stateIn(repositoryScope, SharingStarted.Companion.WhileSubscribed(5000), 10)

    override val canSendQuery: StateFlow<Boolean> = authRepository.authState
        .map { user -> user?.uid }
        .flatMapLatest { uid ->
            if (uid != null) quotaDao.observeQuota(uid).map { it?.hasQuota ?: true }
            else flowOf(false)
        }
        .stateIn(repositoryScope, SharingStarted.Companion.WhileSubscribed(5000), true)


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
                resetAtMillis = networkQuota.resetAt?.toDate()?.time ?: 0L
            )
            quotaDao.saveQuota(entity)
        }
    }

    override suspend fun consumeQuery(userId: String): Boolean {
        var currentQuota = quotaDao.getQuota(userId)

        if (currentQuota?.isExpired == true) {
            val newResetTime = System.currentTimeMillis() + 86400000L

            quotaDao.resetUsage(userId, newResetTime)
            quotaService.resetQuota(userId)

            currentQuota = quotaDao.getQuota(userId)
        }

        if (currentQuota?.hasQuota == false) return false

        quotaDao.incrementUsage(userId)
        quotaService.incrementUsage(userId)

        return true
    }

    override suspend fun onProStatusChanged(userId: String, isPro: Boolean) {
        quotaService.updateLimit(userId, isPro)
        loadQuota(userId, isPro)
    }
}