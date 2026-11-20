package com.yugentech.quill.quota.repository

import com.yugentech.quill.database.dao.QuotaDao
import com.yugentech.quill.database.entity.QuotaEntity
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.BillingRepository
import com.yugentech.quill.domain.QuotaRepository
import com.yugentech.quill.quota.model.QuotaLimits
import com.yugentech.quill.quota.model.todayDateString
import com.yugentech.quill.quota.service.QuotaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class QuotaRepositoryImpl(
    authRepository: AuthRepository,
    private val billingRepository: BillingRepository,
    private val quotaService: QuotaService,
    private val quotaDao: QuotaDao
) : QuotaRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // What's left today for the signed-in account, or null when no one is signed in. The
    // allowance is the plan's, and the plan is whatever Play says right now.
    private val remainingToday: Flow<Int?> = authRepository.authState
        .map { user -> user?.uid }
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                combine(quotaDao.observeQuota(uid), billingRepository.isPro) { quota, isPro ->
                    val limit = QuotaLimits.of(isPro)
                    when {
                        quota == null -> limit
                        // A row left over from a previous day is stale, not exhausted -- show
                        // the full limit rather than yesterday's leftover count. The row itself
                        // gets properly reset the next time a query is actually sent
                        // (consumeQuery), not here -- this is just what the UI displays.
                        quota.isFromPreviousDay(todayDateString()) -> limit
                        else -> quota.remaining(limit)
                    }
                }
            }
        }

    // Kept up to date all the time, not only while a screen watches: a flow that stops between
    // screens hands the next one its last value first, which could be from before a purchase
    // (out of free queries) for a moment, and the chat screen took that as the limit reached
    override val remainingQueries: StateFlow<Int> = remainingToday
        .map { it ?: 0 }
        .stateIn(repositoryScope, SharingStarted.Eagerly, QuotaLimits.FREE)

    override val canSendQuery: StateFlow<Boolean> = remainingToday
        .map { it != null && it > 0 }
        .stateIn(repositoryScope, SharingStarted.Eagerly, true)

    override suspend fun loadQuota(userId: String) {
        val today = todayDateString()
        var networkQuota = quotaService.fetchQuota(userId)

        when {
            networkQuota == null -> {
                quotaService.initQuota(userId)
                networkQuota = quotaService.fetchQuota(userId)
            }

            networkQuota.isFromPreviousDay(today) -> {
                quotaService.resetQuota(userId)
                networkQuota = quotaService.fetchQuota(userId)
            }
        }

        if (networkQuota != null) {
            val entity = QuotaEntity(
                userId = userId,
                queriesUsed = networkQuota.queriesUsed,
                lastResetDate = networkQuota.lastResetDate ?: today
            )
            quotaDao.saveQuota(entity)
        }
    }

    override suspend fun consumeQuery(userId: String, amount: Int): Boolean {
        val today = todayDateString()
        var currentQuota = quotaDao.getQuota(userId)

        if (currentQuota == null || currentQuota.isFromPreviousDay(today)) {
            // Reset both copies against the same "today" -- there's no schedule to invent here,
            // just "is this a new day, yes or no."
            quotaDao.resetUsage(userId, today)
            quotaService.resetQuota(userId)

            currentQuota = quotaDao.getQuota(userId)
        }

        // This runs after the response was already delivered, so it must never refuse to charge.
        // A costly action (e.g. an image) with fewer units left than it costs takes whatever is
        // left, landing exactly on the limit rather than going over it or being skipped for free.
        val limit = QuotaLimits.of(billingRepository.isPro.value)
        val charge = currentQuota?.let { minOf(amount, it.remaining(limit)) } ?: amount
        if (charge <= 0) return false

        quotaDao.incrementUsage(userId, charge)
        quotaService.incrementUsage(userId, charge)

        return true
    }
}
