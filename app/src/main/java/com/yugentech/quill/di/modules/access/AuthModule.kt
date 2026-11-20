package com.yugentech.quill.di.modules.access

import com.yugentech.quill.auth.guest.GuestBackupManager
import com.yugentech.quill.auth.guest.GuestBackupViewModel
import com.yugentech.quill.auth.guest.GuestDataManager
import com.yugentech.quill.auth.guest.GuestSession
import org.koin.android.ext.koin.androidContext

import com.yugentech.quill.auth.repository.AuthRepositoryImpl
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.auth.service.AuthService
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {

    single {
        AuthService(
            auth = get(),
            oneTapClient = get()
        )
    }

    // Guest mode's local ID: guests have no Firebase account
    single { GuestSession(androidContext()) }

    single<AuthRepository> {
        AuthRepositoryImpl(
            authService = get(),
            guestSession = get()
        )
    }

    // Removes a guest's library when they leave guest mode
    single {
        GuestDataManager(
            bookDao = get(),
            userDao = get(),
            cloudSyncRepository = get()
        )
    }

    // Saves a guest's library to a file they pick, and merges such a file back in
    single {
        GuestBackupManager(
            context = androidContext(),
            contentResolver = androidContext().contentResolver,
            bookDao = get(),
            categoryDao = get(),
            highlightDao = get(),
            readingSessionDao = get(),
            userDao = get()
        )
    }

    // Export and import of a guest's library, in More
    viewModel {
        GuestBackupViewModel(
            authRepository = get(),
            backupManager = get(),
            cloudSyncRepository = get(),
            userRepository = get()
        )
    }

    viewModel {
        AuthViewModel(
            authRepository = get(),
            cloudSyncRepository = get(),
            userRepository = get(),
            syncDataStore = get(),
            userDataStore = get(),
            guestDataManager = get()
        )
    }
}