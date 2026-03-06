package com.yugentech.quill.di.modules

import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.quill.auth.repository.AuthRepository
import com.yugentech.quill.auth.repository.AuthRepositoryImpl
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.sessions.auth.service.AuthService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

// Koin module defining dependencies for authentication
val authModule = module {

    // Provides the standard Firebase Auth instance
    single {
        FirebaseAuth.getInstance()
    }

    // Provides the standard Firestore instance
    single {
        FirebaseFirestore.getInstance()
    }

    // Provides the Google Sign-In client for handling One Tap auth
    single {
        Identity.getSignInClient(androidContext())
    }

    // Provides the low-level service that wraps Firebase calls
    single {
        AuthService(
            auth = get(),
            oneTapClient = get()
        )
    }

    // Provides the repository interface used by the UI layer
    single<AuthRepository> {
        AuthRepositoryImpl(
            authService = get()
        )
    }

    viewModel {
        Timber.v("Initializing LoginViewModel")
        AuthViewModel(
            authRepository = get()
        )
    }
}