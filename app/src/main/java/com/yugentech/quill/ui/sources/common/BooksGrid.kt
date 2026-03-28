package com.yugentech.quill.ui.sources.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.ui.sources.standardScreen.components.StandardBookItem

@Composable
fun BooksGrid(
    books: List<Book>,
    isPaginating: Boolean = false,
    onLoadMore: () -> Unit = {},
    onBookClick: (Book) -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            totalItems > 0 && lastVisibleItemIndex >= totalItems - 6
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isPaginating) {
                onLoadMore()
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(
            top = topPadding,
            start = 16.dp,
            end = 16.dp,
            bottom = bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // FIX: Added 'key' and 'animateItem()' so books smoothly slide into place when paginating
        items(
            items = books,
            key = { book -> book.id }
        ) { book ->
            StandardBookItem(
                book = book,
                onClick = { onBookClick(book) },
                modifier = Modifier.animateItem()
            )
        }

        if (isPaginating) {
            // FIX: Give the spinner a hardcoded key so it doesn't get confused during item insertions
            item(key = "pagination_spinner", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}