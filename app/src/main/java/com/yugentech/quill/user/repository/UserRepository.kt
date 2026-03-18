package com.yugentech.quill.user.repository

import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.user.result.UserResult
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String): UserData?
    fun getUserFlow(userId: String): Flow<UserData?>
    suspend fun upsertUser(userData: UserData)
    suspend fun syncUser(userData: UserData): UserResult<Unit>
    suspend fun fetchUserOnce(userId: String): UserResult<Unit>
    suspend fun updateProStatus(userId: String, isPro: Boolean)
}