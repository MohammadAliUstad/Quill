package com.yugentech.quill.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.category.repository.CategoryRepository
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.library.repository.LibraryRepository
import com.yugentech.theme.tokens.AppConstants.FIVE
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val userCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getUserCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FIVE), emptyList())

    val lastReadBook: StateFlow<LibraryBookView?> = libraryRepository.getLastReadBook()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FIVE), null)

    val historyBooks: StateFlow<List<LibraryBookView>> = libraryRepository.getReadingHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FIVE), emptyList())

    val allHistoryBooks: StateFlow<List<LibraryBookView>> =
        libraryRepository.getCompleteReadingHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FIVE), emptyList())

    val favoriteBooks: StateFlow<List<LibraryBookView>> = libraryRepository.getFavoriteBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FIVE), emptyList())

    val bookShelf: StateFlow<List<LibraryBookView>> = libraryRepository.getBookShelf()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FIVE), emptyList())

    fun getBooksForCategory(category: String): StateFlow<List<LibraryBookView>> {
        return libraryRepository.getBooksByCategory(category)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FIVE), emptyList())
    }

    fun initializeDefaultCategories() {
        viewModelScope.launch {
            if (categoryRepository.getCategoryCount() == 0) {
                categoryRepository.initializeDefaultCategories()
            }
        }
    }
}