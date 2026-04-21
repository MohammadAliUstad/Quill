package com.yugentech.quill.ui.shared.bookDetails.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.entity.HighlightEntity
import com.yugentech.quill.reader.repository.ReaderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotesViewModel(
    private val repository: ReaderRepository 
) : ViewModel() {

    private val _annotations = MutableStateFlow<List<HighlightEntity>>(emptyList())
    val annotations = _annotations.asStateFlow()

    // Call this from the UI when the screen opens
    fun loadAnnotations(bookId: String) {
        viewModelScope.launch {
            repository.getHighlights(bookId).collect { highlights ->
                _annotations.value = highlights
            }
        }
    }

    fun deleteAnnotation(highlightId: String) {
        viewModelScope.launch {
            repository.deleteHighlight(highlightId)
        }
    }
}