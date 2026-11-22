package com.yugentech.quill.ui.tabs.discoverScreen.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.yugentech.theme.tokens.dimensions.AppAnimations
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.discover.viewmodel.DiscoverViewModel
import com.yugentech.quill.ui.common.books.BooksGrid
import com.yugentech.quill.ui.common.components.NoSearchResults
import com.yugentech.quill.ui.common.components.SearchStateLayout
import com.yugentech.quill.ui.tabs.discoverScreen.components.AnimatedSearchIcon
import com.yugentech.quill.ui.common.components.rememberSmoothScrimBrush
import com.yugentech.quill.ui.tabs.discoverScreen.components.SearchSuggestions
import com.yugentech.quill.ui.tabs.discoverScreen.components.BookShelfRow
import com.yugentech.quill.ui.common.components.BookShelfSkeleton
import com.yugentech.quill.ui.tabs.discoverScreen.components.DiscoverMaintenanceCard
import com.yugentech.quill.ui.tabs.discoverScreen.components.ShelfHeader
import com.yugentech.quill.ui.tabs.discoverScreen.components.HeroCarousel
import com.yugentech.quill.ui.common.components.HeroCarouselSkeleton
import com.yugentech.quill.ui.common.util.toCoverThumbnailUrl
import com.yugentech.quill.ui.common.components.LibraryParallaxBackground
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import com.yugentech.quill.ui.common.util.getSubtitleForCategory
import com.yugentech.quill.ui.common.util.getTitleForCategory
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun DiscoverScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onBookClick: (Book) -> Unit,
    // A whole shelf: New Arrivals, or a subject by its English name
    onShelfClick: (String) -> Unit,
    // Whether the search is open, so the tab's Subjects button can step aside for it
    onSearchExpandedChange: (Boolean) -> Unit = {},
    viewModel: DiscoverViewModel = koinViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(searchExpanded) { onSearchExpandedChange(searchExpanded) }

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

    val parallaxScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 10000
        }
    }

    var visibleHeroBooks by remember { mutableStateOf<List<Book>>(emptyList()) }

    var parallaxCoverUrl by remember { mutableStateOf<String?>(null) }

    // Whenever the user scrolls (changing the visible books), or every 6 seconds, pick a new background
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
        // The parallax layer
        if (parallaxCoverUrl != null) {
            LibraryParallaxBackground(
                coverUrl = parallaxCoverUrl?.toCoverThumbnailUrl(),
                scrollOffset = parallaxScrollOffset,
                headerHeight = 550.dp
            )
        }

        // The scrolling content
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(
                top = statusBarHeight + 80.dp,
                // 16dp above the dock, as Sessions' tabs: 8 here plus the 8dp every section (the
                // hero, each shelf) already ends with
                bottom = contentPadding.calculateBottomPadding() + 8.dp
            )
        ) {
            item(key = "hero_section") {
                // Part of the first item rather than one of its own, so the parallax (which
                // follows the first item's scroll) moves smoothly past it
                if (uiState.isSourceDown) {
                    DiscoverMaintenanceCard(
                        backAfter = uiState.sourceBackAfter,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                }

                // While the source is down nothing's on its way, so no skeleton waits for it
                if (uiState.isFeedLoading && !uiState.isSourceDown) {
                    HeroCarouselSkeleton(
                        title = stringResource(R.string.new_releases),
                        subtitle = stringResource(R.string.new_releases_subtitle)
                    )
                } else if (uiState.heroBooks.isNotEmpty()) {
                    ShelfHeader(
                        title = stringResource(R.string.new_releases),
                        subtitle = stringResource(R.string.new_releases_subtitle),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )

                    HeroCarousel(
                        books = uiState.heroBooks,
                        onBookClick = onBookClick,
                        onVisibleBooksChanged = { visibleHeroBooks = it }
                    )
                }
            }

            // Category rows
            uiState.categoryRows.forEach { (categoryName, books) ->
                item(key = categoryName) {
                    if (books.isNotEmpty()) {
                        BookShelfRow(
                            title = getTitleForCategory(categoryName),
                            subtitle = getSubtitleForCategory(categoryName),
                            books = books,
                            onBookClick = onBookClick,
                            onSeeAllClick = { onShelfClick(categoryName) }
                        )
                    } else if (!uiState.isSourceDown) {
                        BookShelfSkeleton(
                            title = getTitleForCategory(categoryName),
                            subtitle = getSubtitleForCategory(categoryName)
                        )
                    }
                }
            }
        }

        // The search bar area
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
                    .background(rememberSmoothScrimBrush(surfaceColor))
            )

            // Faded in and out from a see-through copy of the same colour: fading from
            // Color.Transparent (see-through black) passes through dark grey, a flash in light mode
            val animatedContainerColor by animateColorAsState(
                targetValue = if (searchExpanded) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f)
                },
                label = "container_color"
            )

            val animatedBorderColor by animateColorAsState(
                targetValue = if (searchExpanded) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                label = "border_color"
            )

            val hazeAlpha by animateFloatAsState(
                targetValue = if (searchExpanded) 0f else 1f,
                label = "haze_alpha"
            )


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
                        placeholder = { Text(stringResource(R.string.search_titles_authors)) },
                        leadingIcon = {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
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
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    searchText = ""
                                    viewModel.clearSearch()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        },
                        // The frosted backdrop fades out as the bar's own fill fades in, rather
                        // than vanishing on the first frame (which left the field see-through
                        // for a moment: the flicker on tap).
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .hazeEffect(
                                state = hazeState,
                                style = HazeDefaults.style(
                                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                                    blurRadius = 20.dp,
                                    noiseFactor = 0.05f
                                )
                            ) { alpha = hazeAlpha }
                            .border(
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
                // As in Ryori: whatever's showing fades out before the next fades in, so typing
                // doesn't snap the results away, and old books never linger under the spinner
                val searchView = when {
                    searchText.isEmpty() -> SearchView.Suggestions
                    uiState.isSearchLoading -> SearchView.Loading
                    uiState.searchResults.isEmpty() -> SearchView.NoResults
                    else -> SearchView.Results(uiState.searchResults)
                }
                AnimatedContent(
                    targetState = searchView,
                    // New results replace the grid in place, not by a fade
                    contentKey = { it::class },
                    transitionSpec = {
                        fadeIn(
                            tween(
                                durationMillis = AppAnimations.Durations.Smooth,
                                delayMillis = AppAnimations.Durations.Rapid,
                                easing = AppAnimations.Easings.Decelerate
                            )
                        ) togetherWith fadeOut(
                            tween(durationMillis = AppAnimations.Durations.Rapid, easing = AppAnimations.Easings.Accelerate)
                        )
                    },
                    label = "discoverSearch",
                    modifier = Modifier.fillMaxSize()
                ) { view ->
                    when (view) {
                        SearchView.Suggestions -> SearchSuggestions(
                            onSuggestionClick = { suggestion ->
                                searchText = suggestion
                                viewModel.onSearchQuery(suggestion)
                                focusManager.clearFocus()
                            }
                        )

                        // In the icon's spot, with blank text keeping the layout the same height,
                        // so the spinner sits exactly where the no-results icon does
                        SearchView.Loading -> SearchStateLayout(
                            title = "",
                            message = "",
                            icon = { CircularWavyProgressIndicator() }
                        )

                        SearchView.NoResults -> NoSearchResults()

                        is SearchView.Results -> BooksGrid(
                            books = view.books,
                            onBookClick = { book ->
                                // The search stays as it is (query, results, open bar), so
                                // coming back from the book lands on the results again.
                                focusManager.clearFocus()
                                onBookClick(book)
                            },
                            topPadding = 16.dp,
                            // 16dp above the dock, as on the other tabs' lists
                            bottomPadding = contentPadding.calculateBottomPadding() + 16.dp
                        )
                    }
                }
            }
        }
    }
}

/** What the open search shows. */
private sealed interface SearchView {
    data object Suggestions : SearchView
    data object Loading : SearchView
    data object NoResults : SearchView
    data class Results(val books: List<Book>) : SearchView
}
