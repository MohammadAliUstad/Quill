package com.yugentech.quill.auth.guest

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.cloud.repository.CloudSyncRepository
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.user.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Backup rows in Settings. Guests and signed-in users can both export and import. Importing into
 * an account is how a guest's saved file comes into it, and what's added then syncs to that account.
 */
class GuestBackupViewModel(
    private val authRepository: AuthRepository,
    private val backupManager: GuestBackupManager,
    private val cloudSyncRepository: CloudSyncRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    sealed interface Result {
        data object Exported : Result
        data object ExportFailed : Result
        data class Imported(val books: Int, val highlights: Int) : Result
        data object ImportFailed : Result
    }

    data class State(val busy: Boolean = false, val result: Result? = null)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun export(uri: Uri) = launchBackup { userId ->
        if (backupManager.export(userId, uri)) Result.Exported else Result.ExportFailed
    }

    fun import(uri: Uri) = launchBackup { userId ->
        // Signed in, the account's categories came down at sign-in, so the import's name check
        // already sees them and a same-named shelf isn't added twice
        val summary = backupManager.import(userId, uri)
        if (summary == null) {
            Result.ImportFailed
        } else {
            // Signed in: upload what was added. A guest's data stays on the phone. Offline these
            // fail quietly and the next sync picks the rest up, so they don't hold up the result.
            if (!authRepository.isGuest) viewModelScope.launch { syncImported(userId) }
            Result.Imported(summary.booksAdded, summary.highlightsAdded)
        }
    }

    private suspend fun syncImported(userId: String) {
        cloudSyncRepository.syncCategoriesToCloud()
        cloudSyncRepository.syncBooksToCloud()
        // The backup's name is used only when the profile had none, so this is usually unchanged
        userRepository.getUser(userId)?.let { userRepository.syncUser(it) }
    }

    fun dismissResult() = _state.update { it.copy(result = null) }

    private fun launchBackup(block: suspend (userId: String) -> Result) {
        val userId = authRepository.currentUser ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = block(userId)
            _state.update { State(busy = false, result = result) }
        }
    }
}
