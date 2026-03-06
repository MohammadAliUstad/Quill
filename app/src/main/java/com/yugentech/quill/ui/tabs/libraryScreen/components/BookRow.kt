package com.yugentech.quill.ui.tabs.libraryScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.view.LibraryBookView

private const val MAX_ROW_BOOKS = 8

@Composable
fun BookRow(
    title: String,
    books: List<LibraryBookView>,
    onBookClick: (LibraryBookView) -> Unit,
    onSeeAllClick: () -> Unit
) {
    if (books.isEmpty()) return

    val displayedBooks = remember(books) { books.take(MAX_ROW_BOOKS) }
    val hasMore = books.size > MAX_ROW_BOOKS

    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodySmall
    val density = LocalDensity.current

    val cardWidthPx = remember(density) {
        with(density) { 115.dp.toPx().toInt() }
    }

    val rowNeedsTwoLines = remember(displayedBooks, textMeasurer, textStyle, cardWidthPx) {
        displayedBooks.any { book ->
            val layoutResult = textMeasurer.measure(
                text = book.title,
                style = textStyle,
                constraints = Constraints(maxWidth = cardWidthPx),
                maxLines = 2
            )
            layoutResult.lineCount > 1
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (hasMore) {
                TextButton(onClick = onSeeAllClick) {
                    Text("See all")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = displayedBooks,
                key = { it.id }
            ) { book ->
                BookItem(
                    book = book,
                    onClick = { onBookClick(book) },
                    needsTwoLines = rowNeedsTwoLines,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}