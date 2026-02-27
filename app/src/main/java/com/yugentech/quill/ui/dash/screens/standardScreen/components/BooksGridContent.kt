package com.yugentech.quill.ui.dash.screens.standardScreen.components

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.network.domain.Book

@Composable
fun BooksGridContent(
    books: List<Book>,
    isLoading: Boolean,
    onBookClick: (Book) -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    Crossfade(targetState = isLoading, label = "ContentFade") { loading ->
        if (loading) {
            LoadingState()
        } else {
            if (books.isEmpty()) {
                EmptyState()
            } else {
                BooksGrid(
                    books = books,
                    onBookClick = onBookClick,
                    topPadding = topPadding,
                    bottomPadding = bottomPadding
                )
            }
        }
    }
}