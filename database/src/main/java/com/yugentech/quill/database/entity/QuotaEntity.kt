package com.yugentech.quill.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotas")
data class QuotaEntity(
    @PrimaryKey val userId: String,
    val queriesUsed: Int,
    val queriesLimit: Int,
    val resetAtMillis: Long
) {
    val remaining: Int
        get() = (queriesLimit - queriesUsed).coerceAtLeast(0)

    val hasQuota: Boolean
        get() = remaining > 0

    val isExpired: Boolean
        get() = System.currentTimeMillis() > resetAtMillis
}