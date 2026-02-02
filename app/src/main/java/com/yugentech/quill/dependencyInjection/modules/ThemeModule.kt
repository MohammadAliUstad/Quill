package com.yugentech.quill.dependencyInjection.modules

import com.yugentech.quill.theme.ThemeService
import com.yugentech.quill.theme.ThemeViewModel
import com.yugentech.quill.theme.themeRepository.ThemeRepository
import com.yugentech.quill.theme.themeRepository.ThemeRepositoryImpl
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber

// Koin module defining dependencies for UI theming
val themeModule = module {

    // Service that saves and retrieves theme preferences
    single {
        ThemeService(
            dataStore = get(named("theme"))
        )
    }

    // Repository acting as a source of truth for the app's current theme
    single<ThemeRepository> {
        ThemeRepositoryImpl(
            service = get()
        )
    }

    // ViewModel for managing theme selection logic in the UI
    viewModel {
        Timber.v("Initializing ThemeViewModel")
        ThemeViewModel(
            repository = get()
        )
    }
}