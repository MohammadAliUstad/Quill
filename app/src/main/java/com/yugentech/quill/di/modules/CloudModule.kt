package com.yugentech.quill.di.modules

import com.yugentech.quill.cloud.CloudSyncService
import com.yugentech.quill.cloud.repository.CloudSyncRepository
import com.yugentech.quill.cloud.repository.CloudSyncRepositoryImpl
import org.koin.dsl.module

val cloudModule = module {

    single {
        CloudSyncService(
            firestore = get(),
            authRepository = get()
        )
    }

    single<CloudSyncRepository> {
        CloudSyncRepositoryImpl(
            bookDao = get(),
            categoryDao = get(),
            cloudSyncService = get(),
            context = get()
        )
    }
}