package com.yugentech.quill.database.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yugentech.quill.database.model.UserData

@Keep
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String?,
    val email: String?,
    val avatarId: Int?
) {
    fun toUserData(): UserData {
        return UserData(
            userId = userId,
            name = name,
            email = email,
            avatarId = avatarId
        )
    }

    companion object {
        fun fromUserData(userData: UserData): UserEntity {
            return UserEntity(
                userId = userData.userId,
                name = userData.name,
                email = userData.email,
                avatarId = userData.avatarId
            )
        }
    }
}