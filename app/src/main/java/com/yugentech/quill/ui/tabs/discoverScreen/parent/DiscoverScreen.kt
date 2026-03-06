package com.yugentech.quill.ui.tabs.discoverScreen.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.ui.mainScreen.utils.DiscoverViewModel
import com.yugentech.quill.ui.sources.common.AnimatedSearchIcon
import com.yugentech.quill.ui.tabs.discoverScreen.components.BookShelfRow
import com.yugentech.quill.ui.tabs.discoverScreen.components.HeroCarousel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    onBookClick: (Book) -> Unit,
    viewModel: DiscoverViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dockedWidth = screenWidth - 32.dp
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // --- ADDED: Scroll State Tracking ---
    val listState = rememberLazyListState()
    val scrollAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f // Fully visible if scrolled past the first item
            } else {
                // Fade in gradually based on the scroll offset of the top item
                (listState.firstVisibleItemScrollOffset / 100f).coerceIn(0f, 1f)
            }
        }
    }

    BackHandler(enabled = searchActive) {
        searchActive = false
        focusManager.clearFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState, // --- ADDED: Attach state to LazyColumn ---
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 72.dp,
                bottom = 100.dp
            )
        ) {
            if (uiState.heroBooks.isNotEmpty()) {
                item {
                    HeroCarousel(
                        books = uiState.heroBooks,
                        onBookClick = onBookClick,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    if (uiState.standardNewReleases.isNotEmpty()) {
                        BookShelfRow(
                            title = "Fresh from Standard Ebooks",
                            subtitle = "Beautifully formatted public domain classics",
                            books = uiState.standardNewReleases,
                            onBookClick = onBookClick
                        )
                    }
                    if (uiState.gutenbergPopular.isNotEmpty()) {
                        BookShelfRow(
                            title = "Trending on Gutenberg",
                            subtitle = "Most downloaded books today",
                            books = uiState.gutenbergPopular,
                            onBookClick = onBookClick
                        )
                    }
                    if (uiState.sciFiBooks.isNotEmpty()) {
                        BookShelfRow(
                            title = "Sci-Fi Adventures",
                            books = uiState.sciFiBooks,
                            onBookClick = onBookClick
                        )
                    }
                    if (uiState.mysteryBooks.isNotEmpty()) {
                        BookShelfRow(
                            title = "Mystery & Suspense",
                            books = uiState.mysteryBooks,
                            onBookClick = onBookClick
                        )
                    }
                    if (uiState.romanceBooks.isNotEmpty()) {
                        BookShelfRow(
                            title = "Classic Romance",
                            books = uiState.romanceBooks,
                            onBookClick = onBookClick
                        )
                    }
                }
            }
        }

        // Floating search bar layer
        // Floating search bar layer
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
            contentAlignment = Alignment.TopCenter // <-- ADDED THIS: Centers the SearchBar perfectly
        ) {
            // --- ADDED: Gradient Background Layer ---
            // --- ADDED: Gradient Background Layer ---
            val surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Set height to cover the status bar, search bar, and extend a bit for the fade
                    .height(statusBarHeight + 110.dp)
                    .graphicsLayer {
                        alpha = scrollAlpha // Bind opacity to scroll state
                    }
                    .background(
                        Brush.verticalGradient(
                            // Increased alpha values for a stronger background, keeping the curve smooth
                            0.0f to surfaceColor.copy(alpha = 0.98f), // Almost solid at the very top
                            0.3f to surfaceColor.copy(alpha = 0.90f), // Remains strong behind the SearchBar
                            0.6f to surfaceColor.copy(alpha = 0.60f), // Begins to fade smoothly
                            0.8f to surfaceColor.copy(alpha = 0.20f), // Softens the edge
                            1.0f to surfaceColor.copy(alpha = 0.0f)   // Invisible at the very bottom
                        )
                    )
            )

            // Original SearchBar
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        onQueryChange = { searchText = it },
                        onSearch = {
                            searchActive = false
                            focusManager.clearFocus()
                        },
                        expanded = searchActive,
                        onExpandedChange = { searchActive = it },
                        placeholder = { Text("Search titles, authors...") },
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    searchActive = !searchActive
                                    if (!searchActive) focusManager.clearFocus()
                                }
                            ) {
                                AnimatedSearchIcon(isSearchActive = searchActive)
                            }
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                expanded = searchActive,
                onExpandedChange = { searchActive = it },
                modifier = Modifier.widthIn(min = dockedWidth), // Width remains the same
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    dividerColor = MaterialTheme.colorScheme.outlineVariant
                ),
                windowInsets = SearchBarDefaults.windowInsets
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Search results will appear here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}