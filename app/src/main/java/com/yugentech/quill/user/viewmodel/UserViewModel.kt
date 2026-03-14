package com.yugentech.quill.user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.user.repository.UserRepository
import com.yugentech.quill.user.state.UserUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber

class UserViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    fun loadUser(userId: String) {
        if (currentUserId == userId && _uiState.value.user != null) return
        currentUserId = userId

        _uiState.update { it.copy(isLoading = true) }

        userRepository.getUserFlow(userId)
            .filterNotNull()
            .onEach { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }
            .catch { e ->
                Timber.e(e, "Error loading user flow")
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    suspend fun upsertUser(userData: UserData) {
        try {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            userRepository.upsertUser(userData)
            userRepository.syncUser(userData)
            _uiState.update { it.copy(isSaving = false) }
        } catch (e: Exception) {
            Timber.e(e, "Error saving user")
            _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
        }
    }
}