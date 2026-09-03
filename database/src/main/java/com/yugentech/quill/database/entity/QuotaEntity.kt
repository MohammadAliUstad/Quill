package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotas")
data class QuotaEntity(
    @PrimaryKey val userId: String,
    val queriesUsed: Int,
    val queriesLimit: Int,
    // The calendar day (device-local, "yyyy-MM-dd") this queriesUsed count belongs to -- not a
    // reset instant. Comparing this to today's date is the entire reset mechanism: no timestamp
    // math, no invented schedule, nothing that can drift out of sync with the server's own copy
    // of the same comparison.
    val lastResetDate: String
) {
    val remaining: Int
        get() = (queriesLimit - queriesUsed).coerceAtLeast(0)

    val hasQuota: Boolean
        get() = remaining > 0

    fun isFromPreviousDay(today: String): Boolean = lastResetDate != today
}
