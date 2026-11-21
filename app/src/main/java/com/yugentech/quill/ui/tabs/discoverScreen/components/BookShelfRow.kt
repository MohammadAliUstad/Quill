package com.yugentech.quill.ui.tabs.discoverScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Book

@Composable
fun BookShelfRow(
    title: String,
    subtitle: String? = null,
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    // Opens the whole shelf; null when there's nothing more to it than the row
    onSeeAllClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        ShelfHeader(
            title = title,
            subtitle = subtitle,
            onSeeAllClick = onSeeAllClick,
            // Less on the right: the See all button carries its own padding
            modifier = Modifier.padding(start = 16.dp, end = if (onSeeAllClick != null) 4.dp else 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = books,
                key = { book -> book.id }
            ) { book ->
                DiscoverBookCard(
                    book = book,
                    onClick = { onBookClick(book) }
                )
            }
        }
    }
}