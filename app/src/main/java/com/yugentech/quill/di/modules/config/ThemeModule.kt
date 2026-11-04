package com.yugentech.quill.di.modules.config

import com.yugentech.quill.theme.service.ThemeService
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.quill.theme.themeRepository.ThemeRepositoryImpl
import com.yugentech.theme.ThemeRepository
import com.yugentech.quill.user.datastore.UserDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val themeModule = module {

    // The "Haptic Feedback" switch in Settings; QuillTheme gates all UI haptics on it.
    single<Flow<Boolean>>(named("hapticsEnabled")) {
        val userDataStore: UserDataStore = get()
        userDataStore.settingsConfiguration.map { it.hapticsEnabled }
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
            themeRepository = get(),
            hapticsEnabledFlow = get(named("hapticsEnabled"))
        )
    }
}