package com.yugentech.quill.ui.sources.standard.parent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.yugentech.quill.sources.standard.viewmodel.StandardNavigationEvent
import com.yugentech.quill.sources.standard.viewmodel.StandardViewModel
import com.yugentech.quill.ui.sources.standard.components.BooksGrid
import com.yugentech.quill.ui.sources.standard.components.SourceEmptyState
import com.yugentech.quill.ui.sources.standard.components.StandardScreenHeader
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

private enum class SourceScreenState {
    Loading, Empty, Content
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardScreen(
    onNavigateById: (String) -> Unit,
    onNavigateByContent: (Book) -> Unit,
    viewModel: StandardViewModel = koinViewModel()
) {
    val books by viewModel.booksState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val displayTitle by viewModel.displayTitle.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is StandardNavigationEvent.NavigateById -> onNavigateById(event.id)
                is StandardNavigationEvent.NavigateByContent -> onNavigateByContent(event.book)
            }
        }
    }

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val updateSearchActive = { active: Boolean ->
        searchActive = active
        if (!active && searchText.isEmpty()) {
            viewModel.onCategorySelected("New Arrivals")
        }
    }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val dockedWidth = screenWidth - 32.dp

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gridTopPadding = statusBarHeight + 56.dp + 64.dp

    BackHandler(
        enabled = searchActive
    ) {
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
            AnimatedContent(
                targetState = when {
                    isLoading && books.isEmpty() -> SourceScreenState.Loading
                    !isLoading && books.isEmpty() -> SourceScreenState.Empty
                    else -> SourceScreenState.Content
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "StandardContentAnimation"
            ) { state ->
                when (state) {
                    SourceScreenState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator()
                        }
                    }
                    SourceScreenState.Empty -> {
                        SourceEmptyState(
                            title = displayTitle,
                            message = error,
                            modifier = Modifier.padding(top = gridTopPadding)
                        )
                    }
                    SourceScreenState.Content -> {
                        BooksGrid(
                            books = books,
                            topPadding = gridTopPadding,
                            bottomPadding = navBarHeight,
                            onBookClick = { book -> viewModel.onBookClick(book) }
                        )
                    }
                }
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
                onSearchClear = { searchText = "" },
                onBackOrClose = {
                    updateSearchActive(!searchActive)
                },
                onSuggestionClick = { suggestion ->
                    searchText = suggestion
                    viewModel.onSearchQuery(suggestion)
                    searchActive = false
                    focusManager.clearFocus()
                },
                categories = categories,
                selectedCategory = selectedCategory,
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
