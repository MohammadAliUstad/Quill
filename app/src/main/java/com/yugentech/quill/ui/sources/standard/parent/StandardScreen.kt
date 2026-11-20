package com.yugentech.quill.ui.sources.standard.parent

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
import com.yugentech.quill.sources.standard.viewmodel.NEW_ARRIVALS
import com.yugentech.quill.sources.standard.viewmodel.StandardNavigationEvent
import com.yugentech.quill.sources.standard.viewmodel.StandardViewModel
import com.yugentech.quill.ui.main.components.ObserveAsEvents
import com.yugentech.quill.ui.sources.standard.components.BooksGrid
import com.yugentech.quill.ui.sources.standard.components.STANDARD_MIN_CELL
import com.yugentech.quill.ui.sources.standard.components.SourceContentHost
import com.yugentech.quill.ui.sources.standard.components.StandardScreenHeader
import org.koin.androidx.compose.koinViewModel

/**
 * Standard Ebooks: a header with search and the category chips over the books. Everything below
 * the header comes from [StandardViewModel.uiState]; the screen only keeps what's being typed.
 */
@Composable
fun StandardScreen(
    onNavigateById: (String) -> Unit,
    onNavigateByContent: (Book) -> Unit,
    viewModel: StandardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.navigationEvents) { event ->
        when (event) {
            is StandardNavigationEvent.NavigateById -> onNavigateById(event.id)
            is StandardNavigationEvent.NavigateByContent -> onNavigateByContent(event.book)
        }
    }

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val updateSearchActive = { active: Boolean ->
        searchActive = active
        if (!active && searchText.isEmpty()) {
            viewModel.onCategorySelected(NEW_ARRIVALS)
        }
    }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val dockedWidth = screenWidth - 32.dp

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
    // Below the search bar (56) and the category chips (64)
    val gridTopPadding = statusBarHeight + 56.dp + 64.dp

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
            SourceContentHost(
                content = uiState.content,
                skeletonColumns = GridCells.Adaptive(minSize = STANDARD_MIN_CELL),
                topPadding = gridTopPadding,
                bottomPadding = bottomPadding
            ) { books ->
                BooksGrid(
                    books = books,
                    topPadding = gridTopPadding,
                    bottomPadding = bottomPadding,
                    onBookClick = viewModel::onBookClick
                )
            }

            StandardScreenHeader(
                searchText = searchText,
                searchActive = searchActive,
                dockedWidth = dockedWidth,
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
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    viewModel.onCategorySelected(category)
                    if (searchText.isNotEmpty()) searchText = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
            )
        }
    }
}
