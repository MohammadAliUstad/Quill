package com.yugentech.quill.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yugentech.quill.database.entity.QuotaEntity
import kotlinx.coroutines.flow.Flow // <-- Import this

@Dao
interface QuotaDao {
    @Query("SELECT * FROM quotas WHERE userId = :userId")
    fun observeQuota(userId: String): Flow<QuotaEntity?>

    @Query("SELECT * FROM quotas WHERE userId = :userId")
    suspend fun getQuota(userId: String): QuotaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuota(quota: QuotaEntity)

    @Query("UPDATE quotas SET queriesUsed = queriesUsed + 1 WHERE userId = :userId")
    suspend fun incrementUsage(userId: String)

    @Query("UPDATE quotas SET queriesUsed = 0, resetAtMillis = :newResetAt WHERE userId = :userId")
    suspend fun resetUsage(userId: String, newResetAt: Long)

    @Query("UPDATE quotas SET queriesLimit = :newLimit WHERE userId = :userId")
    suspend fun updateLimit(userId: String, newLimit: Int)
}