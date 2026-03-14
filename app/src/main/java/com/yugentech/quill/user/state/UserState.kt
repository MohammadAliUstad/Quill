package com.yugentech.quill.user.state

import com.yugentech.quill.database.model.UserData

data class UserUiState(
    val user: UserData? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)