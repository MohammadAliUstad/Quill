package com.yugentech.quill.di.modules

import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.quill.user.datastore.UserDataStore
import com.yugentech.quill.user.repository.UserRepository
import com.yugentech.quill.user.repository.UserRepositoryImpl
import com.yugentech.quill.user.service.SyncDataStore
import com.yugentech.quill.user.service.UserService
import com.yugentech.quill.user.viewmodel.UserViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
import timber.log.Timber

// Koin module defining dependencies for user profile management
val userModule = module {

    // Provides the Firestore instance
    single { FirebaseFirestore.getInstance() }

    // Service for direct Firestore user document operations
    single {
        UserService(
            firestore = get()
        )
    }

    single {
        Timber.d("Initializing SyncPreferences")
        SyncDataStore(
            dataStore = get(named("sync"))
        )
    }

    // Manages local preferences specific to the user
    single {
        UserDataStore(get(named("user")))
    }

    // Repository that syncs user profile data between local storage and Firestore
    single<UserRepository> {
        UserRepositoryImpl(
            userDao = get(),
            userService = get(),
            syncDataStore = get()
        )
    }

    viewModel {
        UserViewModel(
            userRepository = get()
        )
    }
}