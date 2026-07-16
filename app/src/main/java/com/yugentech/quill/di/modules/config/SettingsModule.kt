package com.yugentech.quill.di.modules.config

import com.yugentech.quill.ui.tabs.moreScreen.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(userDataStore = get(), hapticService = get()) }
}
