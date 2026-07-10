package com.yugentech.quill.ui.tabs.discoverScreen.parent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.discover.viewmodel.DiscoverViewModel
import com.yugentech.quill.ui.sources.gutenberg.components.BooksGrid
import com.yugentech.quill.ui.sources.standard.components.AnimatedSearchIcon
import com.yugentech.quill.ui.sources.standard.components.SearchSuggestions
import com.yugentech.quill.ui.tabs.discoverScreen.components.BookShelfRow
import com.yugentech.quill.ui.tabs.discoverScreen.components.BookShelfSkeleton
import com.yugentech.quill.ui.tabs.discoverScreen.components.HeroCarousel
import com.yugentech.quill.ui.tabs.discoverScreen.components.HeroCarouselSkeleton
import com.yugentech.quill.ui.tabs.libraryScreen.components.LibraryParallaxBackground
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import getSubtitleForCategory
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun DiscoverScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onBookClick: (Book) -> Unit,
    viewModel: DiscoverViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val dockedWidth = screenWidth - 32.dp

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val hazeState = remember { HazeState() }

    val listState = rememberLazyListState()
    val scrollAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 100f).coerceIn(0f, 1f)
            }
        }
    }

    // --- PARALLAX LOGIC START ---
    val parallaxScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 10000
        }
    }

    var visibleHeroBooks by remember { mutableStateOf<List<Book>>(emptyList()) }

    var parallaxCoverUrl by remember { mutableStateOf<String?>(null) }

    // 3. Whenever the user scrolls (changing the visible books), or every 6 seconds, pick a new background
    LaunchedEffect(visibleHeroBooks) {
        if (visibleHeroBooks.isNotEmpty()) {
            // Instantly change to a random visible book when they scroll to it
            parallaxCoverUrl = visibleHeroBooks.random().coverUrl

            // Continue to rotate randomly between the visible ones if they stop scrolling
            while (true) {
                delay(6000)
                parallaxCoverUrl = visibleHeroBooks.random().coverUrl
            }
        } else {
            parallaxCoverUrl = uiState.heroBooks.firstOrNull()?.coverUrl
        }
    }
    // --- PARALLAX LOGIC END ---

    BackHandler(enabled = searchExpanded) {
        searchExpanded = false
        searchText = ""
        viewModel.clearSearch()
        focusManager.clearFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- THE PARALLAX LAYER ---
        if (parallaxCoverUrl != null) {
            LibraryParallaxBackground(
                coverUrl = parallaxCoverUrl,
                scrollOffset = parallaxScrollOffset,
                headerHeight = 550.dp
            )
        }

        // --- THE SCROLLING CONTENT ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(
                top = statusBarHeight + 80.dp,
                bottom = contentPadding.calculateBottomPadding()
            )
        ) {
            item(key = "hero_section") {
                if (uiState.isFeedLoading) {
                    HeroCarouselSkeleton(
                        title = "New Releases",
                        subtitle = "Freshly curated classics for your reading pleasure"
                    )
                } else if (uiState.heroBooks.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "New Releases",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Freshly curated classics for your reading pleasure",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HeroCarousel(
                        books = uiState.heroBooks,
                        onBookClick = onBookClick,
                        onVisibleBooksChanged = { visibleHeroBooks = it }
                    )
                }
            }

            // --- CATEGORY ROWS ---
            uiState.categoryRows.forEach { (categoryName, books) ->
                item(key = categoryName) {
                    if (books.isNotEmpty()) {
                        BookShelfRow(
                            title = categoryName,
                            subtitle = getSubtitleForCategory(categoryName),
                            books = books,
                            onBookClick = onBookClick
                        )
                    } else {
                        BookShelfSkeleton(
                            title = categoryName,
                            subtitle = getSubtitleForCategory(categoryName)
                        )
                    }
                }
            }
        }

        // --- THE SEARCH BAR AREA ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            val surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 110.dp)
                    .graphicsLayer { alpha = scrollAlpha }
                    .background(
                        Brush.verticalGradient(
                            0.0f to surfaceColor.copy(alpha = 0.9f),
                            0.4f to surfaceColor.copy(alpha = 0.7f),
                            0.7f to surfaceColor.copy(alpha = 0.30f),
                            1.0f to surfaceColor.copy(alpha = 0.0f)
                        )
                    )
            )

            val animatedContainerColor by animateColorAsState(
                targetValue = if (searchExpanded) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    Color.Transparent
                },
                label = "container_color"
            )

            val animatedBorderColor by animateColorAsState(
                targetValue = if (searchExpanded) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                label = "border_color"
            )

            if (!searchExpanded) {
                Box(
                    modifier = Modifier
                        .padding(top = statusBarHeight + 8.dp)
                        .widthIn(min = dockedWidth)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeDefaults.style(
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                                blurRadius = 20.dp,
                                noiseFactor = 0.05f
                            )
                        )
                )
            }

            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        onQueryChange = { query ->
                            searchText = query
                            if (query.isNotEmpty()) {
                                viewModel.onSearchQuery(query)
                            } else {
                                viewModel.clearSearch()
                            }
                        },
                        onSearch = { focusManager.clearFocus() },
                        expanded = searchExpanded,
                        onExpandedChange = { searchExpanded = it },
                        placeholder = { Text("Search titles, authors...") },
                        leadingIcon = {
                            IconButton(onClick = {
                                if (searchExpanded) {
                                    searchExpanded = false
                                    searchText = ""
                                    viewModel.clearSearch()
                                    focusManager.clearFocus()
                                } else {
                                    searchExpanded = true
                                }
                            }) {
                                AnimatedSearchIcon(isSearchActive = searchExpanded)
                            }
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchText = ""
                                    viewModel.clearSearch()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = animatedBorderColor,
                            shape = RoundedCornerShape(28.dp)
                        )
                    )
                },
                expanded = searchExpanded,
                onExpandedChange = { isActive ->
                    searchExpanded = isActive
                    if (!isActive) {
                        searchText = ""
                        viewModel.clearSearch()
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.widthIn(min = dockedWidth),
                colors = SearchBarDefaults.colors(
                    containerColor = animatedContainerColor,
                    dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                windowInsets = SearchBarDefaults.windowInsets
            ) {
                if (searchText.isEmpty()) {
                    SearchSuggestions(
                        onSuggestionClick = { suggestion ->
                            searchText = suggestion
                            viewModel.onSearchQuery(suggestion)
                            focusManager.clearFocus()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (uiState.isSearchLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(32.dp)
                            )
                        } else if (uiState.searchResults.isNotEmpty()) {
                            BooksGrid(
                                books = uiState.searchResults,
                                onBookClick = { book ->
                                    searchExpanded = false
                                    searchText = ""
                                    viewModel.clearSearch()
                                    onBookClick(book)
                                },
                                topPadding = 16.dp,
                                bottomPadding = contentPadding.calculateBottomPadding()
                            )
                        } else {
                            Text(
                                text = "No results found for '$searchText'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
