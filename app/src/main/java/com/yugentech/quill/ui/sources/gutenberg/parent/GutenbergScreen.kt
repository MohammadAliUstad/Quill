package com.yugentech.quill.ui.sources.gutenberg.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.gutenberg.viewmodel.GutenbergNavigationEvent
import com.yugentech.quill.sources.gutenberg.viewmodel.GutenbergViewModel
import com.yugentech.quill.ui.main.components.ObserveAsEvents
import com.yugentech.quill.ui.sources.gutenberg.components.BooksGrid
import com.yugentech.quill.ui.sources.gutenberg.components.GUTENBERG_COLUMNS
import com.yugentech.quill.ui.sources.gutenberg.components.GutenbergScreenHeader
import com.yugentech.quill.ui.sources.standard.components.SourceContentHost
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.androidx.compose.koinViewModel

/**
 * Project Gutenberg: a frosted search header over the books. Everything below the header comes
 * from [GutenbergViewModel.uiState]; the screen only keeps what's being typed.
 */
@Composable
fun GutenbergScreen(
    onNavigateById: (String) -> Unit,
    onNavigateByContent: (Book) -> Unit,
    viewModel: GutenbergViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.navigationEvents) { event ->
        when (event) {
            is GutenbergNavigationEvent.NavigateById -> onNavigateById(event.id)
            is GutenbergNavigationEvent.NavigateByContent -> onNavigateByContent(event.book)
        }
    }

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val updateSearchActive = { active: Boolean ->
        searchActive = active
        if (!active && searchText.isEmpty()) {
            viewModel.onSearchQuery("")
        }
    }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val dockedWidth = screenWidth - 32.dp

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
    val hazeState = remember { HazeState() }
    // Below the search bar
    val gridTopPadding = statusBarHeight + 80.dp

    BackHandler(enabled = searchActive) {
        updateSearchActive(false)
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
            // The header frosts whatever scrolls under it
            SourceContentHost(
                content = uiState.content,
                skeletonColumns = GridCells.Fixed(GUTENBERG_COLUMNS),
                topPadding = gridTopPadding,
                bottomPadding = bottomPadding,
                modifier = Modifier.hazeSource(hazeState)
            ) { books ->
                BooksGrid(
                    books = books,
                    topPadding = gridTopPadding,
                    bottomPadding = bottomPadding,
                    isPaginating = uiState.isPaginating,
                    onBookClick = viewModel::onBookClick,
                    onLoadMore = viewModel::loadNextPage,
                    hideLastRow = uiState.hasMorePages
                )
            }

            GutenbergScreenHeader(
                searchText = searchText,
                searchActive = searchActive,
                dockedWidth = dockedWidth,
                hazeState = hazeState,
                statusBarHeight = statusBarHeight,
                onSearchTextChange = { searchText = it },
                onSearchSubmit = { query ->
                    viewModel.onSearchQuery(query)
                    searchActive = false
                    focusManager.clearFocus()
                },
                onSearchActiveChange = { updateSearchActive(it) },
                onSearchClear = {
                    searchText = ""
                    viewModel.onSearchQuery("")
                },
                onBackOrClose = {
                    updateSearchActive(!searchActive)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
            )
        }
    }
}
