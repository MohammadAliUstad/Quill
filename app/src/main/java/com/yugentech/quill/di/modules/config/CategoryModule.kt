package com.yugentech.quill.di.modules.config

import com.yugentech.quill.category.repository.CategoryRepository
import com.yugentech.quill.category.repository.CategoryRepositoryImpl
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val categoryModule = module {

    single<CategoryRepository> {
        CategoryRepositoryImpl(
            categoryDao = get(),
            cloudSyncRepository = get()
        )
    }

    viewModel {
        CategoryViewModel(
            repository = get()
        )
    }
}