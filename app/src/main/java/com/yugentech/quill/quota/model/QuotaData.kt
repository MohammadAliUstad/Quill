package com.yugentech.quill.quota.model

data class QuotaData(
    val queriesUsed: Int,
    val queriesLimit: Int,
    // The calendar day (device-local, "yyyy-MM-dd") this queriesUsed count belongs to.
    val lastResetDate: String?
) {
    fun isFromPreviousDay(today: String): Boolean = lastResetDate != today

    val hasQuota: Boolean
        get() = queriesUsed < queriesLimit

    val remaining: Int
        get() = (queriesLimit - queriesUsed).coerceAtLeast(0)
}
