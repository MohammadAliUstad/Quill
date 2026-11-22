package com.yugentech.quill.ui.common.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Book

/** The smallest a cover gets in the Standard Ebooks grid; as many fit across as will. */
val STANDARD_MIN_CELL = 100.dp

@Composable
fun BooksGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    val gridState = rememberLazyGridState()
    // The covers ripple in once, when this list's grid first appears. Each list (a category,
    // a search) gets its own grid, so refreshes and new pages never replay it
    val entrance = rememberGridEntrance(Unit)

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = STANDARD_MIN_CELL),
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
            items = books,
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
    }
}