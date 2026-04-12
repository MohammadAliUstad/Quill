package com.yugentech.quill.di.modules.books

import com.yugentech.quill.library.repository.LibraryRepository
import com.yugentech.quill.library.repository.LibraryRepositoryImpl
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.ui.info.indexing.viewmodel.IndexingViewModel
import com.yugentech.quill.ui.tabs.libraryScreen.viewmodel.SeeAllViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val booksModule = module {

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

    viewModel { (categoryName: String) ->
        SeeAllViewModel(
            categoryName = categoryName,
            libraryRepository = get()
        )
    }

    viewModel {
        IndexingViewModel(
            workManager = get(),
            bookDao = get()
        )
    }
}