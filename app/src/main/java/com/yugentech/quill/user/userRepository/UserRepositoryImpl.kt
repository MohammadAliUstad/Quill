package com.yugentech.quill.user.userRepository

import com.yugentech.quill.models.UserData
import com.yugentech.quill.room.daos.UserDao
import com.yugentech.quill.room.entities.UserEntity
import com.yugentech.quill.sessions.SyncPreferences
import com.yugentech.quill.user.UserResult
import com.yugentech.quill.user.UserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val userService: UserService,
    private val syncPreferences: SyncPreferences
) : UserRepository {

    override fun getUserFlow(userId: String): Flow<UserData?> {
        // Observe database changes and convert Entity to Domain model
        return userDao.getUserFlow(userId)
            .map { entity -> entity?.toUserData() }
    }

    override suspend fun upsertUser(userData: UserData) {
        try {
            Timber.d("Upserting user locally: ${userData.userId}")
            val entity = UserEntity.fromUserData(userData)
            userDao.saveUser(entity)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upsert user locally")
            throw e
        }
    }

    override suspend fun getUser(userId: String): UserData? {
        return userDao.getUser(userId)?.toUserData()
    }

    override suspend fun syncUser(userData: UserData): UserResult<Unit> {
        Timber.i("Syncing user data to cloud: ${userData.userId}")
        return userService.uploadUser(userData)
    }

    override suspend fun fetchUserOnce(userId: String): UserResult<Unit> {
        return try {
            // Check if we have already fetched the initial data
            val alreadyFetched = syncPreferences.isUserFetchDone().first()

            if (alreadyFetched) {
                return UserResult.Success(Unit)
            }

            Timber.i("Performing initial user profile fetch")
            // Fetch from cloud and save to local database
            when (val result = userService.fetchUser(userId)) {
                is UserResult.Success -> {
                    val userData = result.data
                    Timber.d("Fetched user profile from cloud. Saving locally.")

                    val entity = UserEntity.fromUserData(userData)
                    userDao.saveUser(entity)

                    syncPreferences.setUserFetchDone(true)
                    UserResult.Success(Unit)
                }
                is UserResult.Error -> {
                    Timber.w("Failed to fetch user from cloud: ${result.message}")
                    result
                }
                is UserResult.Loading -> {
                    UserResult.Loading
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during user fetch")
            UserResult.Error(e.message ?: "Failed to fetch user")
        }
    }
}