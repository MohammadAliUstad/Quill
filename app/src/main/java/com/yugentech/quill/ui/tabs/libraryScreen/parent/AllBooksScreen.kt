package com.yugentech.quill.ui.tabs.libraryScreen.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.tabs.libraryScreen.viewmodel.SeeAllViewModel
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.mapper.toBook
import com.yugentech.quill.ui.tabs.libraryScreen.components.BookItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllBooksScreen(
    onBackClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    viewModel: SeeAllViewModel
) {
    val title by viewModel.title.collectAsState()
    val books by viewModel.books.collectAsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = (-scrollBehavior.state.contentOffset / 100f).coerceIn(0f, 1f)
                        }
                        .background(
                            Brush.verticalGradient(
                                0.0f to surfaceColor.copy(alpha = 0.9f),
                                0.4f to surfaceColor.copy(alpha = 0.7f),
                                0.7f to surfaceColor.copy(alpha = 0.30f),
                                1.0f to surfaceColor.copy(alpha = 0.0f)
                            )
                        )
                )

                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 115.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = books,
                key = { it.id }
            ) { book ->
                BookItem(
                    book = book,
                    onClick = { onBookClick(book.toBook()) }
                )
            }
        }
    }
}