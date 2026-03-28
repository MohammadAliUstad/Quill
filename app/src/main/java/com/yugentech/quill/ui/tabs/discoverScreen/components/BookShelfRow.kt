package com.yugentech.quill.ui.tabs.discoverScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Book

@Composable
fun BookShelfRow(
    title: String,
    subtitle: String? = null,
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FIX: Added the 'key' so Compose knows exactly how to track insertions/deletions
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