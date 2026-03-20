package com.yugentech.quill.quota

import com.yugentech.quill.domain.QuotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class QuotaRepositoryImpl(
    private val quotaService: QuotaService
) : QuotaRepository {

    private val _remainingQueries = MutableStateFlow(0)
    override val remainingQueries: StateFlow<Int> = _remainingQueries.asStateFlow()

    private val _canSendQuery = MutableStateFlow(false)
    override val canSendQuery: StateFlow<Boolean> = _canSendQuery.asStateFlow()


    override suspend fun loadQuota(userId: String, isPro: Boolean) {
        var quota = quotaService.fetchQuota(userId)

        if (isPro) {
            updateState(remaining = 100, canSend = true)
        }

        when {
            quota == null -> {
                Timber.d("No quota document found, initializing for user: $userId")
                quotaService.initQuota(userId, isPro)
                quota = quotaService.fetchQuota(userId)
            }
            quota.isExpired -> {
                Timber.d("Quota expired, resetting for user: $userId")
                quotaService.resetQuota(userId)
                quota = quotaService.fetchQuota(userId)
            }
        }

        updateState(quota?.remaining ?: 0, quota?.hasQuota ?: false)
    }

    override suspend fun consumeQuery(userId: String): Boolean {
        if (!_canSendQuery.value) {
            Timber.d("Query blocked — quota exhausted for user: $userId")
            return false
        }

        quotaService.incrementUsage(userId)

        val newRemaining = (_remainingQueries.value - 1).coerceAtLeast(0)
        updateState(newRemaining, newRemaining > 0)

        Timber.d("Query consumed for user: $userId remaining=$newRemaining")
        return true
    }

    override suspend fun onProStatusChanged(userId: String, isPro: Boolean) {
        Timber.d("Pro status changed for user: $userId isPro=$isPro")
        quotaService.updateLimit(userId, isPro)
        loadQuota(userId, isPro)
    }

    // Private — not part of the interface
    private fun updateState(remaining: Int, canSend: Boolean) {
        _remainingQueries.value = remaining
        _canSendQuery.value = canSend
    }
}