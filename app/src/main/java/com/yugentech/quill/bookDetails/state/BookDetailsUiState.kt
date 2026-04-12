package com.yugentech.quill.bookDetails.state

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.Chapter

data class BookDetailsUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = true,
    val isDescriptionExpanded: Boolean = true,
    val isPro: Boolean = false
)