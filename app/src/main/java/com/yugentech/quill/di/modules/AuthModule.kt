package com.yugentech.quill.di.modules

import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.yugentech.quill.auth.repository.AuthRepositoryImpl
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.sessions.auth.service.AuthService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {

    single {
        FirebaseAuth.getInstance()
    }

    single {
        FirebaseFirestore.getInstance()
    }

    single {
        FirebaseFunctions.getInstance(
            app = FirebaseApp.getInstance(),
            regionOrCustomDomain = "us-central1"
        )
    }

    single {
        Identity.getSignInClient(androidContext())
    }

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