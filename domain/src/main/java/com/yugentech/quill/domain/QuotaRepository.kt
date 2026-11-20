package com.yugentech.quill.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Aira's daily query count. Only what's been used (and for which day) is stored; the allowance
 * follows from whether Google Play says the account has Pro.
 */
interface QuotaRepository {
    val remainingQueries: StateFlow<Int>
    val canSendQuery: StateFlow<Boolean>
    suspend fun loadQuota(userId: String)
    suspend fun consumeQuery(userId: String, amount: Int = 1): Boolean
}
