package com.yugentech.quill.auth.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yugentech.quill.auth.state.AuthState
import com.yugentech.quill.cloud.repository.CloudSyncRepository
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.AuthResult
import com.yugentech.quill.ui.auth.state.ForgotPasswordState
import com.yugentech.quill.user.repository.UserRepository
import com.yugentech.quill.user.result.UserResult
import com.yugentech.quill.user.service.SyncDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val syncDataStore: SyncDataStore
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(isInitializing = true))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _forgotPasswordState =
        MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    private var profileLoadingJob: Job? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { firebaseUser ->
                if (firebaseUser != null) {
                    Timber.d("Auth state update: User logged in ${firebaseUser.uid}")
                    FirebaseCrashlytics.getInstance().setUserId(firebaseUser.uid)

                    // Only load the profile if we haven't already loaded it for this specific user
                    if (_authState.value.userId != firebaseUser.uid) {
                        loadUserProfile(firebaseUser)
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
                            userData = null,
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
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    // Focused category sync on successful sign in
                    cloudSyncRepository.syncLibraryOnLogin()
                }

                is AuthResult.Error -> {
                    Timber.w("Sign in failed: ${result.message}")
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        _authState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val result = authRepository.signUp(name, email, password)) {
                is AuthResult.Success -> {
                    Timber.i("Sign up successful, creating local profile")
                    val firebaseUser = result.data
                    val newUser = UserData(
                        userId = firebaseUser.uid,
                        name = name,
                        email = email,
                        avatarId = (1..27).random()
                    )

                    syncOrCreateUser(newUser)
                    // Ensure categories are synced/initialized for the new user
                    cloudSyncRepository.syncLibraryOnLogin()
                }

                is AuthResult.Error -> {
                    Timber.w("Sign up failed: ${result.message}")
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
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
            when (val result = authRepository.handleGoogleSignInResult(data)) {
                is AuthResult.Success -> {
                    cloudSyncRepository.syncLibraryOnLogin()
                }

                is AuthResult.Error -> {
                    Timber.w("Google Sign-In failed: ${result.message}")
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun signOut() {
        Timber.i("User requested sign out")
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Wipe local category data and sync flags on sign out
            cloudSyncRepository.wipeLocalData()
            syncDataStore.clearSyncFlags()
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

    // --- Profile Management Logic ---

    private fun loadUserProfile(firebaseUser: FirebaseUser) {
        profileLoadingJob?.cancel()
        profileLoadingJob = viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }

            when (userRepository.fetchUserOnce(firebaseUser.uid)) {
                is UserResult.Success -> {
                    val localUser = userRepository.getUser(firebaseUser.uid)
                    if (localUser != null) {
                        Timber.d("Local profile loaded for ${firebaseUser.uid}")
                        _authState.update {
                            it.copy(
                                isInitializing = false,
                                isLoading = false,
                                isUserLoggedIn = true,
                                userId = firebaseUser.uid,
                                userData = localUser
                            )
                        }
                    } else {
                        Timber.e("Profile missing after fetch success")
                        _authState.update {
                            it.copy(
                                isInitializing = false,
                                isLoading = false,
                                error = "Failed to load local profile"
                            )
                        }
                    }
                }

                is UserResult.Error -> {
                    Timber.w("Profile fetch failed, attempting creation")
                    val newUser = UserData(
                        userId = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "Reader",
                        email = firebaseUser.email ?: "",
                        avatarId = (1..27).random()
                    )
                    syncOrCreateUser(newUser)
                }

                is UserResult.Loading -> {}
            }
        }
    }

    private suspend fun syncOrCreateUser(userData: UserData) {
        try {
            userRepository.upsertUser(userData)
            when (val syncResult = userRepository.syncUser(userData)) {
                is UserResult.Success -> {
                    _authState.update {
                        it.copy(
                            isInitializing = false,
                            isLoading = false,
                            isUserLoggedIn = true,
                            userId = userData.userId,
                            userData = userData
                        )
                    }
                }

                is UserResult.Error -> {
                    Timber.e("User sync failed: ${syncResult.message}")
                    _authState.update {
                        it.copy(
                            isInitializing = false,
                            isLoading = false,
                            error = "Sync failed: ${syncResult.message}"
                        )
                    }
                }

                else -> {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Critical error creating user profile")
            _authState.update {
                it.copy(
                    isInitializing = false,
                    isLoading = false,
                    error = "Profile creation failed: ${e.message}"
                )
            }
        }
    }
}