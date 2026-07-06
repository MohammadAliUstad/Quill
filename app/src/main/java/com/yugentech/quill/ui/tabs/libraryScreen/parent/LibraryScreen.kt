package com.yugentech.quill.ui.tabs.libraryScreen.parent

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
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
import com.yugentech.quill.ui.tabs.libraryScreen.components.BookRow
import com.yugentech.quill.ui.tabs.libraryScreen.components.HistoryCarousel
import com.yugentech.quill.ui.tabs.libraryScreen.components.LastReadBookCard
import com.yugentech.quill.ui.tabs.libraryScreen.components.LibraryParallaxBackground
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onLibraryBookClick: (Book) -> Unit,
    onResumeClick: (Book) -> Unit,
    onSeeAllClick: (categoryName: String) -> Unit,
    viewModel: LibraryViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val lastReadBook by viewModel.lastReadBook.collectAsState()
    val historyBooks by viewModel.historyBooks.collectAsState()
    val allHistoryBooks by viewModel.allHistoryBooks.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val bookShelf by viewModel.bookShelf.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()

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

    LaunchedEffect(scrollState.maxValue) {
        if (savedScroll > 0 && scrollState.maxValue >= savedScroll) {
            scrollState.scrollTo(savedScroll)
        }
    }

    LaunchedEffect(Unit) { viewModel.initializeDefaultCategories() }

    val allCategoryBooks = userCategories.map { category ->
        val booksFlow = remember(category.name) { viewModel.getBooksForCategory(category.name) }
        booksFlow.collectAsState().value
    }

    val isEmpty = !isInitializing &&
            lastReadBook == null &&
            historyBooks.isEmpty() &&
            favoriteBooks.isEmpty() &&
            bookShelf.isEmpty() &&
            allCategoryBooks.all { it.isEmpty() }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

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
                            text = "Library",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->

        // Wrap the content in a Crossfade to smooth out the transition when data loads
        Crossfade(
            targetState = isEmpty,
            animationSpec = tween(durationMillis = 800), // Smooth 800ms fade transition
            label = "library_content_transition"
        ) { currentlyEmpty ->

            if (currentlyEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding() + 8.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.empty_library),
                            contentDescription = "Empty library",
                            modifier = Modifier.size(240.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Your library is empty",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Books you download or add\nwill appear here",
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
                                coverUrl = parallaxCoverUrl,
                                scrollOffset = scrollState.value,
                                headerHeight = headerHeight
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding() + 8.dp
                            ),
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
                                    hazeState = hazeState,
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
                            val categoryBooks by booksFlow.collectAsState(initial = emptyList())

                            if (categoryBooks.isNotEmpty()) {
                                BookRow(
                                    title = category.name,
                                    books = categoryBooks,
                                    onBookClick = { onLibraryBookClick(it.toBook()) },
                                    onSeeAllClick = { onSeeAllClick(category.name) }
                                )
                            }
                        }

                        if (favoriteBooks.isNotEmpty()) {
                            BookRow(
                                title = "Favorites",
                                books = favoriteBooks,
                                onBookClick = { onLibraryBookClick(it.toBook()) },
                                onSeeAllClick = { onSeeAllClick("Favorites") }
                            )
                        }

                        if (bookShelf.isNotEmpty()) {
                            BookRow(
                                title = "My Shelf",
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