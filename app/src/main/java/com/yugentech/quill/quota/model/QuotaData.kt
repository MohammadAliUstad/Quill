package com.yugentech.quill.quota.model

import com.google.firebase.Timestamp
import java.util.Date

data class QuotaData(
    val queriesUsed: Int,
    val queriesLimit: Int,
    val resetAt: Timestamp?
) {
    val isExpired: Boolean
        get() = resetAt != null && resetAt.toDate().before(Date())

    val hasQuota: Boolean
        get() = queriesUsed < queriesLimit

    val remaining: Int
        get() = (queriesLimit - queriesUsed).coerceAtLeast(0)
}