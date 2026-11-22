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
            val uploadData = userData.toMap()

            profileDocRef(userData.userId).set(uploadData).await()
            UserResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload user profile")
            UserResult.Error(AuthErrorMapper.mapFirebaseAuthError(e))
        }
    }

    suspend fun fetchUser(userId: String): UserResult<UserData> {
        return try {
            val document = profileDocRef(userId).get().await()

            if (!document.exists()) {
                Timber.w("User profile document not found")
                return UserResult.Error("User not found")
            }

            val userData = UserData(
                userId = document.getString("userId") ?: userId,
                name = document.getString("name"),
                email = document.getString("email"),
                avatarId = document.getLong("avatarId")?.toInt() ?: 0
            )

            UserResult.Success(userData)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user profile")
            UserResult.Error(AuthErrorMapper.mapFirebaseAuthError(e))
        }
    }
}