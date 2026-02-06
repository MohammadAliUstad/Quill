package com.yugentech.quill.dependencyInjection.modules

import com.yugentech.quill.theme.ThemeService
import com.yugentech.quill.theme.ThemeViewModel
import com.yugentech.quill.theme.themeRepository.ThemeRepository
import com.yugentech.quill.theme.themeRepository.ThemeRepositoryImpl
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val themeModule = module {

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