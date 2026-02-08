package com.yugentech.quill.ui.dash.screens.standardScreen.components

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.yugentech.quill.network.domain.Book

@Composable
fun BooksGridContent(
    books: List<Book>,
    isLoading: Boolean,
    onBookClick: (Book) -> Unit
) {
    Crossfade(targetState = isLoading, label = "ContentFade") { loading ->
        if (loading) {
            LoadingState()
        } else {
            if (books.isEmpty()) {
                EmptyState()
            } else {
                BooksGrid(books = books, onBookClick = onBookClick)
            }
        }
    }
}