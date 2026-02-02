package com.yugentech.quill.user.utils

import com.yugentech.quill.models.UserData

// Represents the UI state for user-related screens
data class UserState(
    val user: UserData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)