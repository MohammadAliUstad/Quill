package com.yugentech.quill.ui.sources.gutenberg.parent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.gutenberg.viewmodel.GutenbergNavigationEvent
import com.yugentech.quill.sources.gutenberg.viewmodel.GutenbergViewModel
import com.yugentech.quill.ui.sources.gutenberg.components.BooksGrid
import com.yugentech.quill.ui.sources.gutenberg.components.GutenbergScreenHeader
import com.yugentech.quill.ui.sources.standardScreen.components.AnimatedSearchIcon
import com.yugentech.quill.ui.sources.standardScreen.components.SearchSuggestions
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GutenbergScreen(
    onBackClick: () -> Unit,
    onNavigateByContent: (Book) -> Unit,
    viewModel: GutenbergViewModel = koinViewModel()
) {
    val books by viewModel.booksState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is GutenbergNavigationEvent.NavigateByContent -> onNavigateByContent(event.book)
            }
        }
    }

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val dockedWidth = screenWidth - 32.dp

    // Only needed for the grid offset
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val gridTopPadding = statusBarHeight + 80.dp

    BackHandler(enabled = searchActive) {
        searchActive = false
        searchText = ""
        viewModel.onSearchQuery("")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // LAYER 1: GRID
            AnimatedVisibility(
                visible = books.isNotEmpty() || isLoading,
                enter = fadeIn(animationSpec = tween(durationMillis = 300))
            ) {
                BooksGrid(
                    books = books,
                    topPadding = gridTopPadding,
                    bottomPadding = navBarHeight,
                    onBookClick = { book -> viewModel.onBookClick(book) },
                    onLoadMore = { viewModel.loadNextPage() }
                )
            }

            if (isLoading && books.isEmpty()) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            GutenbergScreenHeader(
                searchText = searchText,
                searchActive = searchActive,
                dockedWidth = dockedWidth,
                onSearchTextChange = { searchText = it },
                onSearchSubmit = { query ->
                    viewModel.onSearchQuery(query)
                    searchActive = false
                    focusManager.clearFocus()
                },
                onSearchActiveChange = { searchActive = it },
                onSearchClear = {
                    searchText = ""
                    viewModel.onSearchQuery("")
                },
                onBackOrClose = {
                    if (searchActive) {
                        searchActive = false
                        searchText = ""
                        viewModel.onSearchQuery("")
                    } else {
                        onBackClick()
                    }
                },
                leadingIcon = { AnimatedSearchIcon(isSearchActive = searchActive) },
                searchContent = {
                    SearchSuggestions(
                        onSuggestionClick = { suggestion ->
                            searchText = suggestion
                            viewModel.onSearchQuery(suggestion)
                            searchActive = false
                            focusManager.clearFocus()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
            )
        }
    }
}