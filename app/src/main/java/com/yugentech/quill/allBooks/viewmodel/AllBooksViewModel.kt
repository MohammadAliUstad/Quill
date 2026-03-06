package com.yugentech.quill.allBooks.viewmodel

import androidx.lifecycle.ViewModel
import com.yugentech.quill.allBooks.AllBooksArgs
import com.yugentech.quill.database.view.LibraryBookView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AllBooksViewModel : ViewModel() {

    private val _title = MutableStateFlow(AllBooksArgs.title)
    val title: StateFlow<String> = _title.asStateFlow()

    private val _books = MutableStateFlow(AllBooksArgs.books)
    val books: StateFlow<List<LibraryBookView>> = _books.asStateFlow()
}