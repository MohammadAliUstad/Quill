package com.yugentech.quill.sources.discover.state

import com.yugentech.quill.database.model.Book
import java.time.Instant

data class DiscoverUiState(
    val heroBooks: List<Book> = emptyList(),
    val categoryRows: Map<String, List<Book>> = emptyMap(),
    val isFeedLoading: Boolean = true,
    val isSearchActive: Boolean = false,
    val isSearchLoading: Boolean = false,
    val searchResults: List<Book> = emptyList(),
    // Every subject in the catalogue (English names, the keys), for Browse by subject
    val subjects: List<String> = emptyList(),
    // Standard Ebooks is down for maintenance, back after [sourceBackAfter] if it said when
    val isSourceDown: Boolean = false,
    val sourceBackAfter: Instant? = null
)