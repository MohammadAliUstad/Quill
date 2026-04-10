package com.yugentech.quill.di.modules.access

import com.yugentech.quill.auth.repository.AuthRepositoryImpl
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.sessions.auth.service.AuthService
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {

    single {
        AuthService(
            auth = get(),
            oneTapClient = get()
        )
    }

    single<AuthRepository> {
        AuthRepositoryImpl(
            authService = get()
        )
    }

    viewModel {
        AuthViewModel(
            authRepository = get(),
            cloudSyncRepository = get(),
            userRepository = get(),
            syncDataStore = get()
        )
    }
}