package com.yugentech.quill.bookDetails.state

import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.Chapter
import java.time.Instant

data class BookDetailsUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = true,
    val isDescriptionExpanded: Boolean = true,
    /**
     * The book's source is down for maintenance, so it can't be downloaded until it's back
     * (after [sourceBackAfter], if the source said when).
     */
    val isSourceDown: Boolean = false,
    val sourceBackAfter: Instant? = null,
    /** How far the running download has got, 0..1, when that's known; null otherwise. */
    val downloadProgress: Float? = null
)