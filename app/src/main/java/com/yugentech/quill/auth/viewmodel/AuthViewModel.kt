package com.yugentech.quill.auth.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.yugentech.quill.auth.guest.GuestDataManager
import com.yugentech.quill.auth.state.AuthState
import com.yugentech.quill.cloud.repository.CloudSyncRepository
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.domain.AuthResult
import com.yugentech.quill.domain.AuthUser
import com.yugentech.quill.ui.access.signIn.state.ForgotPasswordState
import com.yugentech.quill.user.repository.UserRepository
import com.yugentech.quill.user.result.UserResult
import com.yugentech.quill.user.service.SyncDataStore
import com.yugentech.quill.user.datastore.UserDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import com.yugentech.quill.R
import com.yugentech.quill.localization.AppStrings

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val syncDataStore: SyncDataStore,
    private val userDataStore: UserDataStore,
    private val guestDataManager: GuestDataManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(isInitializing = true))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val showOnboarding = userDataStore.isOnboardingCompleted
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _forgotPasswordState =
        MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    private var profileLoadingJob: Job? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                if (user != null) {
                    // A guest's ID never leaves the phone, crash reports included
                    FirebaseCrashlytics.getInstance().setUserId(if (user.isGuest) "" else user.uid)

                    if (_authState.value.userId != user.uid) {
                        loadUserProfile(user)
                    }
                } else {
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
                    val firebaseUser = result.data
                    val newUser = UserData(
                        userId = firebaseUser.uid,
                        name = name,
                        email = email,
                        avatarId = (1..27).random()
                    )

                    syncOrCreateUser(newUser)
                    cloudSyncRepository.syncLibraryOnLogin()
                }

                is AuthResult.Error -> {
                    Timber.w("Sign up failed: ${result.message}")
                    _authState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userDataStore.saveOnboardingCompleted(true)
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

    // Guest mode
    // A guest exists only on this phone: a local ID, no Firebase account, and a library that
    // never leaves the device. Aira, Pro and cloud sync need an account. To keep their reading
    // in an account they export a backup file, leave guest mode, sign in, and import it there.

    /** Starts guest mode; works offline. The auth state then loads it like any sign-in. */
    fun continueAsGuest() {
        _authState.update { it.copy(isLoading = true, error = null) }
        authRepository.startGuest()
    }

    /**
     * Leaves guest mode: removes the guest's library and profile from this phone, then their
     * local ID. The auth state then shows the sign-in screen, as after signing out.
     */
    fun exitGuestMode() {
        val guestId = authRepository.currentUser ?: return
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                guestDataManager.deleteLocalData(guestId)
                syncDataStore.clearSyncFlags()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Stay in guest mode rather than leave the library behind for the next account
                // (the books table isn't per account). Clearing isLoading lets them try again.
                Timber.e(e, "Couldn't remove guest data; staying in guest mode")
                _authState.update {
                    it.copy(isLoading = false, error = AppStrings.get(R.string.error_generic))
                }
                return@launch
            }
            authRepository.endGuest()
        }
    }

    fun signOut() {
        _authState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
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

    private fun loadUserProfile(user: AuthUser) {
        profileLoadingJob?.cancel()
        profileLoadingJob = viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            val isGuest = user.isGuest

            when (userRepository.fetchUserOnce(user.uid)) {
                is UserResult.Success -> {
                    val localUser = userRepository.getUser(user.uid)
                    if (localUser != null) {
                        _authState.update {
                            it.copy(
                                isInitializing = false,
                                isLoading = false,
                                isUserLoggedIn = true,
                                isGuest = isGuest,
                                userId = user.uid,
                                userData = localUser
                            )
                        }
                    } else {
                        Timber.e("Profile missing after fetch success")
                        _authState.update {
                            it.copy(
                                isInitializing = false,
                                isLoading = false,
                                error = AppStrings.get(R.string.profile_load_failed)
                            )
                        }
                    }
                }

                is UserResult.Error -> {
                    // Offline with a profile already on this phone (a guest reopening the app,
                    // say): use it rather than making a new one over their name and avatar
                    val localUser = userRepository.getUser(user.uid)
                    if (localUser != null) {
                        Timber.w("Profile fetch failed; using the profile on this device")
                        _authState.update {
                            it.copy(
                                isInitializing = false,
                                isLoading = false,
                                isUserLoggedIn = true,
                                isGuest = isGuest,
                                userId = user.uid,
                                userData = localUser
                            )
                        }
                        return@launch
                    }

                    Timber.w("Profile fetch failed, attempting creation")
                    // A guest has no name or email yet; the app shows "Guest" until they pick a name
                    val newUser = UserData(
                        userId = user.uid,
                        name = if (isGuest) null else user.displayName ?: "Reader",
                        email = if (isGuest) null else user.email ?: "",
                        avatarId = (1..27).random()
                    )
                    syncOrCreateUser(newUser, isGuest)
                }

                is UserResult.Loading -> {}
            }
        }
    }

    private suspend fun syncOrCreateUser(userData: UserData, isGuest: Boolean = false) {
        try {
            userRepository.upsertUser(userData)
            when (val syncResult = userRepository.syncUser(userData)) {
                is UserResult.Success -> {
                    _authState.update {
                        it.copy(
                            isInitializing = false,
                            isLoading = false,
                            isUserLoggedIn = true,
                            isGuest = isGuest,
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
                            error = AppStrings.get(R.string.sync_failed, syncResult.message.orEmpty())
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
                    error = AppStrings.get(R.string.profile_creation_failed, e.message.orEmpty())
                )
            }
        }
    }
}