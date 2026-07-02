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

private const val COLUMNS = 3

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BooksGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    isPaginating: Boolean = false,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    onLoadMore: (() -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()

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

    // Always pad to complete the last row, regardless of pagination state.
    // This keeps the last row stable during load transitions and prevents
    // the 2-book flash when isPaginating toggles.
    val trailingPadding = if (books.isNotEmpty()) {
        val rem = books.size % COLUMNS
        if (rem != 0) COLUMNS - rem else 0
    } else 0

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

        repeat(trailingPadding) { index ->
            item(key = "padding_$index") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.65f),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {}
            }
        }

        if (isPaginating) {
            item(
                key = "loading_indicator",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }
        }
    }
}
