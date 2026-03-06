package com.yugentech.quill.ui.tabs.libraryScreen.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.database.mapper.toBook
import com.yugentech.quill.ui.tabs.libraryScreen.components.BookRow
import com.yugentech.quill.ui.tabs.libraryScreen.components.HistoryCarousel
import com.yugentech.quill.ui.tabs.libraryScreen.components.LastReadBookCard
import com.yugentech.quill.ui.tabs.libraryScreen.components.LibraryParallaxBackground
import kotlinx.coroutines.delay

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onLibraryBookClick: (Book) -> Unit,
    onResumeClick: (Book) -> Unit,
    onSeeAllClick: (title: String, books: List<LibraryBookView>) -> Unit,
    viewModel: LibraryViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val lastReadBook by viewModel.lastReadBook.collectAsState()
    val historyBooks by viewModel.historyBooks.collectAsState()
    val allHistoryBooks by viewModel.allHistoryBooks.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val bookShelf by viewModel.bookShelf.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()

    LaunchedEffect(Unit) { viewModel.initializeDefaultCategories() }

    val scrollState = rememberScrollState()
    val headerHeight = 550.dp

    var currentBackgroundIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(allHistoryBooks) {
        if (allHistoryBooks.isNotEmpty()) {
            while (true) {
                delay(8000)
                currentBackgroundIndex = (currentBackgroundIndex + 1) % allHistoryBooks.size
            }
        }
    }

    val parallaxCoverUrl = allHistoryBooks.getOrNull(currentBackgroundIndex)?.coverUrl
        ?: lastReadBook?.coverUrl

    Box(modifier = modifier.fillMaxSize()) {
        if (parallaxCoverUrl != null) {
            LibraryParallaxBackground(
                coverUrl = parallaxCoverUrl,
                scrollState = scrollState,
                headerHeight = headerHeight
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (lastReadBook != null) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Continue Reading",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LastReadBookCard(
                        book = lastReadBook!!,
                        onCardClick = { onResumeClick(lastReadBook!!.toBook()) },
                        onCoverClick = { onLibraryBookClick(it.toBook()) }
                    )
                }
            }

            if (historyBooks.isNotEmpty()) {
                Column {
                    Text(
                        text = "Recently Read",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                    )
                    HistoryCarousel(
                        books = historyBooks,
                        onBookClick = { onLibraryBookClick(it.toBook()) }
                    )
                }
            }

            userCategories.forEach { category ->
                val booksFlow = remember(category.name) {
                    viewModel.getBooksForCategory(category.name)
                }
                val categoryBooks by booksFlow.collectAsState()

                if (categoryBooks.isNotEmpty()) {
                    BookRow(
                        title = category.name,
                        books = categoryBooks,
                        onBookClick = { onLibraryBookClick(it.toBook()) },
                        onSeeAllClick = { onSeeAllClick(category.name, categoryBooks) }
                    )
                }
            }

            if (favoriteBooks.isNotEmpty()) {
                BookRow(
                    title = "Favorites",
                    books = favoriteBooks,
                    onBookClick = { onLibraryBookClick(it.toBook()) },
                    onSeeAllClick = { onSeeAllClick("Favorites", favoriteBooks) }
                )
            }

            if (bookShelf.isNotEmpty()) {
                BookRow(
                    title = "My Shelf",
                    books = bookShelf,
                    onBookClick = { onLibraryBookClick(it.toBook()) },
                    onSeeAllClick = { onSeeAllClick("My Shelf", bookShelf) }
                )
            }
        }
    }
}