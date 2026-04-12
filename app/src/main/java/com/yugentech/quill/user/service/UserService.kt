package com.yugentech.quill.user.service

import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.quill.auth.mapper.AuthErrorMapper
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.user.result.UserResult
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class UserService(
    private val firestore: FirebaseFirestore
) {

    private fun profileDocRef(userId: String) = firestore.collection("users").document(userId)

    suspend fun uploadUser(userData: UserData): UserResult<Unit> {
        return try {
            Timber.d("Uploading user profile for: ${userData.userId}")
            val uploadData = userData.toMap()

            profileDocRef(userData.userId).set(uploadData).await()
            Timber.i("User profile uploaded successfully")
            UserResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload user profile")
            UserResult.Error(AuthErrorMapper.mapFirebaseAuthError(e))
        }
    }

    suspend fun fetchUser(userId: String): UserResult<UserData> {
        return try {
            Timber.d("Fetching user profile for: $userId")
            val document = profileDocRef(userId).get().await()

            if (!document.exists()) {
                Timber.w("User profile document not found")
                return UserResult.Error("User not found")
            }

            val userData = UserData(
                userId = document.getString("userId") ?: userId,
                name = document.getString("name"),
                email = document.getString("email"),
                avatarId = document.getLong("avatarId")?.toInt() ?: 0,
                isPro = document.getBoolean("isPro") ?: false
            )

            Timber.i("User profile fetched successfully")
            UserResult.Success(userData)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user profile")
            UserResult.Error(AuthErrorMapper.mapFirebaseAuthError(e))
        }
    }

    suspend fun updateProStatus(userId: String, isPro: Boolean): UserResult<Unit> {
        return try {
            Timber.d("Updating Pro status in Firestore for: $userId to $isPro")
            profileDocRef(userId).update("isPro", isPro).await()
            UserResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update pro status in Firestore")
            UserResult.Error(AuthErrorMapper.mapFirebaseAuthError(e))
        }
    }
}