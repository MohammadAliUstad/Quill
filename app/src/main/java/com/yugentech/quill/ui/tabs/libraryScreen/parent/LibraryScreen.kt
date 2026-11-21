package com.yugentech.quill.ui.tabs.libraryScreen.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.database.mapper.toBook
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.ui.shared.scrim.rememberSmoothScrimBrush
import com.yugentech.quill.ui.tabs.libraryScreen.components.BookRow
import com.yugentech.quill.ui.tabs.libraryScreen.components.HistoryCarousel
import com.yugentech.quill.ui.tabs.libraryScreen.components.LastReadBookCard
import com.yugentech.quill.ui.tabs.discoverScreen.util.toCoverThumbnailUrl
import com.yugentech.quill.ui.tabs.libraryScreen.components.LibraryParallaxBackground
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.ui.tabs.libraryScreen.components.shelfTitle
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalDensity
import com.yugentech.quill.library.viewmodel.LibraryState

/** The library's content rises in as the sign-in screens' blocks do: this long, this far. */
private const val LIBRARY_RISE_MS = 550
private val LIBRARY_RISE_DISTANCE = 24.dp

/** Switching between the books and the empty state, the old one fades out over this first. */
private const val LIBRARY_FADE_OUT_MS = 120

/** Material 3's emphasized-decelerate easing, as the sign-in screens' rise-in uses. */
private val LibraryRiseEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onLibraryBookClick: (Book) -> Unit,
    onResumeClick: (Book) -> Unit,
    onSeeAllClick: (categoryName: String) -> Unit,
    viewModel: LibraryViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollToBottom: Boolean = false,
    onScrollToBottomHandled: () -> Unit = {},
    onEmptyStateChange: (Boolean) -> Unit = {}
) {
    val lastReadBook by viewModel.lastReadBook.collectAsState()
    val historyBooks by viewModel.historyBooks.collectAsState()
    val allHistoryBooks by viewModel.allHistoryBooks.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val bookShelf by viewModel.bookShelf.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()
    val libraryState by viewModel.libraryState.collectAsState()

    val scrollState = rememberScrollState()

    // THE FIX: Coercion Protection Strategy.
    var savedScroll by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(scrollState) {
        var wasInProgress = false
        snapshotFlow { scrollState.isScrollInProgress }.collect { inProgress ->
            if (wasInProgress && !inProgress) {
                savedScroll = scrollState.value
            }
            wasInProgress = inProgress
        }
    }

    LaunchedEffect(scrollState.maxValue, scrollToBottom) {
        // Skip while a scroll-to-bottom request is pending -- otherwise this fires the moment
        // maxValue settles and races the effect below back to wherever the user scrolled to
        // *last* time they were on this tab, undoing the jump to the bottom.
        if (!scrollToBottom && savedScroll > 0 && scrollState.maxValue >= savedScroll) {
            scrollState.scrollTo(savedScroll)
        }
    }

    // After a book import, glide down to the very bottom so the user sees where the freshly
    // added book went. "My Shelf" was unsubscribed while the user was away on the Sources tab,
    // so it re-queries from scratch on remount -- give that a moment to land before measuring.
    LaunchedEffect(scrollToBottom) {
        if (scrollToBottom) {
            try {
                delay(400)
                // If rows are still arriving and the end moves while it glides, carry on to the
                // new end. A touch stops it (the scroll is cancelled), handled below all the same.
                while (scrollState.value < scrollState.maxValue) {
                    val distance = scrollState.maxValue - scrollState.value
                    scrollState.animateScrollTo(
                        value = scrollState.maxValue,
                        animationSpec = tween(
                            durationMillis = (distance / 3).coerceIn(450, 1100),
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            } finally {
                savedScroll = scrollState.value
                onScrollToBottomHandled()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeDefaultCategories()
    }

    // Lets the parent swap the Continue FAB for an Add Books FAB while the library is empty
    // (once that's known: nothing changes while it's still being worked out).
    LaunchedEffect(libraryState) {
        if (libraryState != LibraryState.Loading) onEmptyStateChange(libraryState == LibraryState.Empty)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val riseDistancePx = with(LocalDensity.current) { LIBRARY_RISE_DISTANCE.roundToPx() }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val surfaceColor = MaterialTheme.colorScheme.surface

            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = (scrollState.value / 100f).coerceIn(0f, 1f)
                        }
                        .background(rememberSmoothScrimBrush(surfaceColor))
                )

                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.tab_library),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->

        // The books (or the empty state) rise in as the sign-in screens' blocks do: the screen
        // fades in while its content slides up into place (the slide is on the content itself,
        // via riseIn below, so the cover backdrop only fades and never shows a gap). The same
        // when it switches between the two (the first book added, the last one removed), with
        // the old one fading out quickly first.
        AnimatedContent(
            targetState = libraryState,
            transitionSpec = {
                if (initialState == LibraryState.Loading) {
                    fadeIn(tween(LIBRARY_RISE_MS, easing = LibraryRiseEasing)) togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(LIBRARY_RISE_MS - LIBRARY_FADE_OUT_MS, delayMillis = LIBRARY_FADE_OUT_MS, easing = LibraryRiseEasing)) togetherWith
                        fadeOut(tween(LIBRARY_FADE_OUT_MS))
                }
            },
            label = "library_content_transition"
        ) { state ->
            val riseIn = Modifier.animateEnterExit(
                enter = slideInVertically(tween(LIBRARY_RISE_MS, easing = LibraryRiseEasing)) { riseDistancePx },
                exit = ExitTransition.None
            )

            if (state == LibraryState.Loading) {
                Box(modifier = Modifier.fillMaxSize())
            } else if (state == LibraryState.Empty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding() + 16.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = riseIn,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.empty_library),
                            contentDescription = stringResource(R.string.library_empty_cd),
                            modifier = Modifier.size(240.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.library_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.library_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val headerHeight = 550.dp

                var currentBackgroundIndex by remember { mutableIntStateOf(0) }
                LaunchedEffect(allHistoryBooks) {
                    if (allHistoryBooks.isNotEmpty()) {
                        while (true) {
                            delay(6000)
                            currentBackgroundIndex =
                                (currentBackgroundIndex + 1) % allHistoryBooks.size
                        }
                    }
                }

                val parallaxCoverUrl = allHistoryBooks.getOrNull(currentBackgroundIndex)?.coverUrl
                    ?: lastReadBook?.coverUrl

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (parallaxCoverUrl != null) {
                        Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState)) {
                            LibraryParallaxBackground(
                                coverUrl = parallaxCoverUrl?.toCoverThumbnailUrl(),
                                scrollOffset = scrollState.value,
                                headerHeight = headerHeight
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(riseIn)
                            .verticalScroll(scrollState)
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding() + 16.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (lastReadBook != null) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = stringResource(R.string.continue_reading),
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
                                    text = stringResource(R.string.recently_read),
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
                            val categoryBooks by booksFlow.collectAsState(initial = emptyList())

                            if (categoryBooks.isNotEmpty()) {
                                BookRow(
                                    title = shelfTitle(category.name),
                                    books = categoryBooks,
                                    onBookClick = { onLibraryBookClick(it.toBook()) },
                                    onSeeAllClick = { onSeeAllClick(category.name) }
                                )
                            }
                        }

                        if (favoriteBooks.isNotEmpty()) {
                            BookRow(
                                title = stringResource(R.string.shelf_favorites),
                                books = favoriteBooks,
                                onBookClick = { onLibraryBookClick(it.toBook()) },
                                onSeeAllClick = { onSeeAllClick("Favorites") }
                            )
                        }

                        if (bookShelf.isNotEmpty()) {
                            BookRow(
                                title = stringResource(R.string.shelf_my_shelf),
                                books = bookShelf,
                                onBookClick = { onLibraryBookClick(it.toBook()) },
                                onSeeAllClick = { onSeeAllClick("My Shelf") }
                            )
                        }
                    }
                }
            }
        }
    }
}