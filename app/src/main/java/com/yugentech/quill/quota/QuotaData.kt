package com.yugentech.quill.quota

import com.google.firebase.Timestamp

data class QuotaData(
    val queriesUsed: Int,
    val queriesLimit: Int,
    val resetAt: Timestamp?
) {
    val isExpired: Boolean
        get() = resetAt != null && resetAt.toDate().before(java.util.Date())

    val hasQuota: Boolean
        get() = queriesUsed < queriesLimit

    val remaining: Int
        get() = (queriesLimit - queriesUsed).coerceAtLeast(0)
}