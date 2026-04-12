package com.yugentech.quill.database.dao

import androidx.room.*
import com.yugentech.quill.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun saveUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserFlow(userId: String): Flow<UserEntity?>
}