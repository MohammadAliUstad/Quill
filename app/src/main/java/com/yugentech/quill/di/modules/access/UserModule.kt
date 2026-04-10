package com.yugentech.quill.di.modules.access

import com.yugentech.quill.user.datastore.UserDataStore
import com.yugentech.quill.user.repository.UserRepository
import com.yugentech.quill.user.repository.UserRepositoryImpl
import com.yugentech.quill.user.service.SyncDataStore
import com.yugentech.quill.user.service.UserService
import com.yugentech.quill.user.viewmodel.UserViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val userModule = module {

    single {
        UserService(
            firestore = get()
        )
    }

    single {
        UserDataStore(
            dataStore = get(named("user"))
        )
    }

    single {
        SyncDataStore(
            dataStore = get(named("sync"))
        )
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
}