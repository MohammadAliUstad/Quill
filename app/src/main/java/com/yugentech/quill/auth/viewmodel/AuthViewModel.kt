package com.yugentech.quill.auth.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yugentech.quill.auth.repository.AuthRepository
import com.yugentech.quill.auth.state.AuthState
import com.yugentech.sessions.auth.result.AuthResult
import com.yugentech.quill.ui.auth.state.ForgotPasswordState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(isInitializing = true))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _forgotPasswordState =
        MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { firebaseUser ->
                if (firebaseUser != null) {
                    Timber.d("Auth state update: User logged in ${firebaseUser.uid}")
                    FirebaseCrashlytics.getInstance().setUserId(firebaseUser.uid)
                    _authState.update {
                        AuthState(
                            isInitializing = false,
                            isLoading = false,
                            isUserLoggedIn = true,
                            userId = firebaseUser.uid,
                            error = null
                        )
                    }
                } else {
                    Timber.d("Auth state update: User logged out")
                    FirebaseCrashlytics.getInstance().setUserId("")
                    _authState.update {
                        AuthState(
                            isInitializing = false,
                            isLoading = false,
                            isUserLoggedIn = false,
                            userId = null,
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            if (result is AuthResult.Error) {
                Timber.w("Sign in failed: ${result.message}")
                _authState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signUp(name, email, password)
            if (result is AuthResult.Error) {
                Timber.w("Sign up failed: ${result.message}")
                _authState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun getGoogleSignInIntent(webClientId: String) {
        _authState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.getGoogleSignInIntent(webClientId)) {
                is AuthResult.Success -> {
                    _authState.update { it.copy(isLoading = false, intent = result.data) }
                }

                is AuthResult.Error -> {
                    Timber.e("Failed to get Google Intent: ${result.message}")
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        _authState.update { it.copy(isLoading = true, error = null, intent = null) }
        viewModelScope.launch {
            val result = authRepository.handleGoogleSignInResult(data)
            if (result is AuthResult.Error) {
                Timber.w("Google Sign-In failed: ${result.message}")
                _authState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun signOut() {
        Timber.i("User requested sign out")
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun forgotPassword(email: String) {
        _forgotPasswordState.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> {
                    Timber.i("Password reset email sent to $email")
                    _forgotPasswordState.value = ForgotPasswordState.Success
                }

                is AuthResult.Error -> {
                    Timber.w("Password reset failed: ${result.message}")
                    _forgotPasswordState.value = ForgotPasswordState.Error(result.message)
                }
            }
        }
    }

    fun clearForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }
}