package com.yugentech.quill.ui.sources.gutenberg.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.ui.sources.standard.components.StandardBookItem
import com.yugentech.quill.ui.sources.standard.components.BookCoverPlaceholder
import com.yugentech.quill.ui.sources.standard.components.gridEntrance
import com.yugentech.quill.ui.sources.standard.components.rememberGridEntrance

/** Covers across in the Gutenberg grid (and its loading skeleton). */
const val GUTENBERG_COLUMNS = 3
private const val COLUMNS = GUTENBERG_COLUMNS

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BooksGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    isPaginating: Boolean = false,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    onLoadMore: (() -> Unit)? = null,
    hideLastRow: Boolean = false,
) {
    val gridState = rememberLazyGridState()
    // The covers ripple in once, when this list's grid first appears. Each list (a category,
    // a search) gets its own grid, so refreshes and new pages never replay it
    val entrance = rememberGridEntrance(Unit)

    // While more pages are coming, hold back the final row: it only appears once the next
    // page has loaded beneath it (and the loading indicator is gone), so the grid never ends
    // on a half-filled or placeholder row mid-feed.
    val visibleBooks = if (hideLastRow && books.size > COLUMNS) {
        val lastRowCount = books.size % COLUMNS
        books.dropLast(if (lastRowCount == 0) COLUMNS else lastRowCount)
    } else books

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 12
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore?.invoke()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(COLUMNS),
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
        itemsIndexed(
            items = visibleBooks,
            key = { _, book -> book.id }
        ) { index, book ->
            StandardBookItem(
                book = book,
                onClick = { onBookClick(book) },
                modifier = Modifier
                    .animateItem()
                    .gridEntrance(entrance, index)
            )
        }

        if (isPaginating) {
            // The next page on its way: a row of pulsing covers where its books will land
            items(count = COLUMNS, key = { index -> "loading_cover_$index" }) {
                BookCoverPlaceholder(modifier = Modifier.animateItem())
            }
        }
    }
}
