package com.yugentech.quill.di.modules.config

import com.yugentech.quill.theme.service.ThemeService
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.quill.theme.themeRepository.ThemeRepositoryImpl
import com.yugentech.theme.ThemeRepository
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