package com.yugentech.quill.di.modules

import com.google.firebase.firestore.FirebaseFirestore
import com.yugentech.quill.insghts.InsightsRepository
import com.yugentech.quill.insghts.InsightsRepositoryImpl
import com.yugentech.quill.insghts.InsightsViewModel
import com.yugentech.quill.user.datastore.UserDataStore
import com.yugentech.quill.user.repository.UserRepository
import com.yugentech.quill.user.repository.UserRepositoryImpl
import com.yugentech.quill.user.service.SyncDataStore
import com.yugentech.quill.user.service.UserService
import com.yugentech.quill.user.viewmodel.UserViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val userModule = module {

    single { FirebaseFirestore.getInstance() }

    single {
        UserService(
            firestore = get()
        )
    }

    single {
        SyncDataStore(
            dataStore = get(named("sync"))
        )
    }

    single {
        UserDataStore(get(named("user")))
    }

    single<UserRepository> {
        UserRepositoryImpl(
            userDao = get(),
            userService = get(),
            syncDataStore = get()
        )
    }

    viewModel {
        UserViewModel(
            userRepository = get(),
            insightsRepository = get()
        )
    }

    single<InsightsRepository> {
        InsightsRepositoryImpl(
            bookDao = get(),
            readingSessionDao = get(),
            airaMessageDao = get()
        )
    }

    viewModel {
        InsightsViewModel(
            insightsRepository = get()
        )
    }
}