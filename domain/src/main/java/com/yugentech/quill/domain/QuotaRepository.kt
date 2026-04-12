package com.yugentech.quill.domain

import kotlinx.coroutines.flow.StateFlow

interface QuotaRepository {
    val remainingQueries: StateFlow<Int>
    val canSendQuery: StateFlow<Boolean>
    suspend fun loadQuota(userId: String, isPro: Boolean)
    suspend fun consumeQuery(userId: String): Boolean
    suspend fun onProStatusChanged(userId: String, isPro: Boolean)
}