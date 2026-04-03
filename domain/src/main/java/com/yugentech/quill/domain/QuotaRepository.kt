package com.yugentech.quill.domain

import kotlinx.coroutines.flow.StateFlow

interface QuotaRepository {

    // Emits the number of queries remaining (today for Pro, lifetime for Free) — for UI display
    val remainingQueries: StateFlow<Int>

    // Emits true if the user can send an Aira message right now
    val canSendQuery: StateFlow<Boolean>

    // Called on app start and when auth state changes to load/sync quota into memory
    suspend fun loadQuota(userId: String, isPro: Boolean)

    // Called before every Aira message send — returns false if quota is exhausted
    suspend fun consumeQuery(userId: String): Boolean

    // Called when isPro flips (e.g., user buys subscription) — updates cloud and local DB
    suspend fun onProStatusChanged(userId: String, isPro: Boolean)
}