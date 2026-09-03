package com.yugentech.quill.domain

import kotlinx.coroutines.flow.StateFlow

interface QuotaRepository {
    val remainingQueries: StateFlow<Int>
    val canSendQuery: StateFlow<Boolean>
    suspend fun loadQuota(userId: String, isPro: Boolean)
    suspend fun consumeQuery(userId: String, amount: Int = 1): Boolean
    suspend fun onProStatusChanged(userId: String, isPro: Boolean)
}