package com.yugentech.quill.auth.state

import android.app.PendingIntent
import com.yugentech.quill.database.model.UserData

// Data class holding the current state of the authentication UI
data class AuthState(
    val isInitializing: Boolean = true,
    val isLoading: Boolean = false,
    val userId: String? = null,
    val error: String? = null,
    val intent: PendingIntent? = null,
    val userData: UserData? = null,
    val isUserLoggedIn: Boolean = false
)