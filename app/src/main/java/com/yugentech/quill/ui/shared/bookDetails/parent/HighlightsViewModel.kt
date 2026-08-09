package com.yugentech.quill.ui.shared.bookDetails.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.reader.repository.book.ReaderBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HighlightsViewModel(
    private val repository: ReaderBookRepository 
) : ViewModel() {

    private val _highlights = MutableStateFlow<List<HighlightEntity>>(emptyList())
    val highlights = _highlights.asStateFlow()

    // Call this from the UI when the screen opens
    fun loadHighlights(bookId: String) {
        viewModelScope.launch {
            repository.getHighlights(bookId).collect { highlights ->
                _highlights.value = highlights
            }
        }
    }

    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch {
            repository.deleteHighlight(highlightId)
        }
    }
}