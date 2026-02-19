package com.yugentech.quill.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.yugentech.quill.network.domain.LibraryBookView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Shared in-memory store — set from LibraryScreen before navigating,
// read by AllBooksScreen immediately after. Avoids serializing the full
// book list into the nav route (which would hit argument size limits).
object AllBooksArgs {
    var title: String = ""
    var books: List<LibraryBookView> = emptyList()
}

class AllBooksViewModel : ViewModel() {

    private val _title = MutableStateFlow(AllBooksArgs.title)
    val title: StateFlow<String> = _title.asStateFlow()

    private val _books = MutableStateFlow(AllBooksArgs.books)
    val books: StateFlow<List<LibraryBookView>> = _books.asStateFlow()
}