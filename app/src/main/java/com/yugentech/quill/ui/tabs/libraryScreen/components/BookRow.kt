package com.yugentech.quill.ui.tabs.libraryScreen.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.ui.common.books.BookItem

private const val MAX_ROW_BOOKS = 8

@Composable
fun BookRow(
    title: String,
    books: List<LibraryBookView>,
    onBookClick: (LibraryBookView) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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

    val listState = rememberLazyListState()

    // Glide back to the start when a different book takes the front (one just added or read),
    // but not when the row is only composed again -- coming back from a book's page relaunches
    // every effect, and scrolling then threw away where the user had left the row. The last
    // front book is saved alongside the row's scroll, so it survives that trip too.
    val firstId = displayedBooks.firstOrNull()?.id
    var lastFirstId by rememberSaveable { mutableStateOf(firstId) }
    LaunchedEffect(firstId) {
        if (firstId != lastFirstId) {
            lastFirstId = firstId
            if (firstId != null) listState.animateScrollToItem(0)
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
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    onSeeAllClick()
                }) {
                    Text(stringResource(R.string.see_all))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            state = listState,
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