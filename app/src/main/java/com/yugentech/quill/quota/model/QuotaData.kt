package com.yugentech.quill.quota.model

data class QuotaData(
    val queriesUsed: Int,
    // The calendar day (device-local, "yyyy-MM-dd") this queriesUsed count belongs to.
    val lastResetDate: String?
) {
    fun isFromPreviousDay(today: String): Boolean = lastResetDate != today
}
