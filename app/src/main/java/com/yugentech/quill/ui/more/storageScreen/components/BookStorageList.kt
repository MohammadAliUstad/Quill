package com.yugentech.quill.ui.more.storageScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.model.BookStorageBreakdown

@Composable
fun BookStorageList(
    modifier: Modifier = Modifier,
    books: List<BookEntity>,
    breakdowns: Map<String, BookStorageBreakdown>,
    onDeleteClick: (BookEntity) -> Unit
) {
    if (books.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No books currently downloaded.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(
                items = books,
                key = { _, book -> book.id }
            ) { index, book ->

                val isFirst = index == 0
                val isLast = index == books.lastIndex

                val shape = when {
                    isFirst && isLast -> RoundedCornerShape(16.dp)
                    isFirst -> RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    )

                    isLast -> RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )

                    else -> RoundedCornerShape(4.dp)
                }

                BookStorageItem(
                    book = book,
                    breakdown = breakdowns[book.id],
                    shape = shape,
                    onDeleteClick = { onDeleteClick(book) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}