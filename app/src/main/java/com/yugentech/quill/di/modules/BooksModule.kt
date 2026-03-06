package com.yugentech.quill.di.modules

import androidx.work.WorkManager
import com.yugentech.quill.category.repository.CategoryRepository
import com.yugentech.quill.category.repository.CategoryRepositoryImpl
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.allBooks.viewmodel.AllBooksViewModel
import com.yugentech.quill.library.repository.LibraryRepository
import com.yugentech.quill.library.repository.LibraryRepositoryImpl
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.ui.mainScreen.utils.DiscoverViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val booksModule = module {

    single {
        WorkManager.getInstance(androidContext())
    }
    
    single<LibraryRepository> {
        LibraryRepositoryImpl(
            bookDao = get()
        )
    }

    viewModel {
        LibraryViewModel(
            libraryRepository = get(),
            categoryRepository = get()
        )
    }

    single<CategoryRepository> {
        CategoryRepositoryImpl(
            categoryDao = get()
        )
    }

    viewModel {
        DiscoverViewModel(
            standardRepository = get(),
            gutenbergRepository = get()
        )
    }


    viewModel {
        CategoryViewModel(
            repository = get()
        )
    }



    viewModel {
        AllBooksViewModel()
    }
}