package com.yugentech.quill.quota

import com.google.firebase.Timestamp
import java.util.Date

data class QuotaData(
    val queriesUsed: Int,
    val queriesLimit: Int,
    val resetAt: Timestamp?,
    val isLifetime: Boolean = false
) {
    val isExpired: Boolean
        get() = !isLifetime && resetAt != null && resetAt.toDate().before(Date())

    val hasQuota: Boolean
        get() = queriesUsed < queriesLimit

    val remaining: Int
        get() = (queriesLimit - queriesUsed).coerceAtLeast(0)
}