package com.yugentech.quill.viewmodel.seeAll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.library.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class SeeAllViewModel(
    categoryName: String,
    libraryRepository: LibraryRepository
) : ViewModel() {

    private val _title = MutableStateFlow(categoryName)
    val title: StateFlow<String> = _title.asStateFlow()

    val books: StateFlow<List<LibraryBookView>> = when (categoryName) {
        "Favorites" -> libraryRepository.getFavoriteBooks()
        "My Shelf" -> libraryRepository.getBookShelf()
        else -> libraryRepository.getBooksByCategory(categoryName)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}