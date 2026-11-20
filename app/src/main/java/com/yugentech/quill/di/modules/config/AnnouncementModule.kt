package com.yugentech.quill.di.modules.config

import com.yugentech.quill.announcements.datastore.AnnouncementDataStore
import com.yugentech.quill.announcements.repository.AnnouncementRepository
import com.yugentech.quill.announcements.repository.AnnouncementRepositoryImpl
import com.yugentech.quill.announcements.viewmodel.AnnouncementViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val announcementModule = module {

    single {
        AnnouncementDataStore(
            dataStore = get(named("announcements"))
        )
    }

    single<AnnouncementRepository> {
        AnnouncementRepositoryImpl(
            announcementDataStore = get(),
            userDataStore = get()
        )
    }

    viewModel {
        AnnouncementViewModel(
            repository = get()
        )
    }
}
