package com.yugentech.quill.discover

import com.yugentech.quill.database.model.Book

data class DiscoverUiState(
    val heroBooks: List<Book> = emptyList(),
    val categoryRows: Map<String, List<Book>> = emptyMap(),
    val isFeedLoading: Boolean = true,
    val isSearchActive: Boolean = false,
    val isSearchLoading: Boolean = false,
    val searchResults: List<Book> = emptyList()
)