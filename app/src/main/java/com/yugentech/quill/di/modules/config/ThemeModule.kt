package com.yugentech.quill.di.modules.config

import com.yugentech.quill.theme.service.ThemeService
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.quill.theme.themeRepository.ThemeRepositoryImpl
import com.yugentech.theme.ThemeRepository
import com.yugentech.quill.user.datastore.UserDataStore
import com.yugentech.theme.service.HapticService
import kotlinx.coroutines.flow.map
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val themeModule = module {

    single {
        val userDataStore: UserDataStore = get()
        HapticService(
            context = androidContext(),
            hapticsEnabledFlow = userDataStore.settingsConfiguration.map { it.hapticsEnabled }
        )
    }

    single {
        ThemeService(
            dataStore = get(named("theme"))
        )
    }

    single<ThemeRepository> {
        ThemeRepositoryImpl(
            themeService = get()
        )
    }

    viewModel {
        ThemeViewModel(
            themeRepository = get()
        )
    }
}