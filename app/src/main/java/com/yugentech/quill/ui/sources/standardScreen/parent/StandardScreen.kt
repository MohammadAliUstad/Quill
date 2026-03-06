package com.yugentech.quill.ui.sources.standardScreen.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.standardEBooks.viewmodel.StandardNavigationEvent
import com.yugentech.quill.standardEBooks.viewmodel.StandardViewModel
import com.yugentech.quill.ui.sources.common.AnimatedSearchIcon
import com.yugentech.quill.ui.sources.common.BooksGridContent
import com.yugentech.quill.ui.sources.standardScreen.components.CollectionsGridContent
import com.yugentech.quill.ui.sources.standardScreen.components.SearchSuggestions
import com.yugentech.quill.ui.sources.standardScreen.components.StandardScreenHeader
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardScreen(
    onBackClick: () -> Unit,
    onNavigateById: (String) -> Unit,
    onNavigateByContent: (Book) -> Unit,
    standardViewModel: StandardViewModel = koinViewModel()
) {
    val books by standardViewModel.booksState.collectAsState()
    val collections by standardViewModel.collectionsState.collectAsState()
    val isLoading by standardViewModel.isLoading.collectAsState()
    val isPaginating by standardViewModel.isPaginating.collectAsState()
    val selectedCategory by standardViewModel.selectedCategory.collectAsState()
    val categories by standardViewModel.categories.collectAsState()

    LaunchedEffect(key1 = true) {
        standardViewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is StandardNavigationEvent.NavigateById -> onNavigateById(event.id)
                is StandardNavigationEvent.NavigateByContent -> onNavigateByContent(event.book)
            }
        }
    }

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dockedWidth = screenWidth - 32.dp

    // Header height = status bar + search bar (56dp) + filter row (48dp) + small padding
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // statusBar + searchBar + filterRow (including its 8dp top + 8dp bottom padding = 64dp total)
    val gridTopPadding = statusBarHeight + 56.dp + 64.dp
    val gridBottomPadding = navBarHeight + 16.dp

    BackHandler(enabled = searchActive) {
        searchActive = false
        if (searchText.isEmpty()) standardViewModel.onCategorySelected("New Arrivals")
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
            // --- LAYER 1: SCROLLABLE GRID ---
            if (selectedCategory == "Collections") {
                AnimatedVisibility(
                    visible = collections.isNotEmpty() || isLoading,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300))
                ) {
                    CollectionsGridContent(
                        collections = collections,
                        isLoading = isLoading,
                        topPadding = gridTopPadding,
                        bottomPadding = gridBottomPadding,
                        onCollectionClick = { title, url ->
                            standardViewModel.onCollectionSelected(title, url)
                        }
                    )
                }
            } else {
                AnimatedVisibility(
                    visible = books.isNotEmpty() || isLoading,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300))
                ) {
                    BooksGridContent(
                        books = books,
                        isLoading = isLoading,
                        topPadding = gridTopPadding,
                        bottomPadding = gridBottomPadding,
                        onBookClick = { book -> standardViewModel.onBookClick(book) }
                    )
                }
            }

            // --- LAYER 2: UNIFIED HEADER (search bar + filter row, solid background) ---
            StandardScreenHeader(
                searchText = searchText,
                searchActive = searchActive,
                dockedWidth = dockedWidth,
                onSearchTextChange = { searchText = it },
                onSearchSubmit = { query ->
                    standardViewModel.onSearchQuery(query)
                    searchActive = false
                    focusManager.clearFocus()
                },
                onSearchActiveChange = { searchActive = it },
                onSearchClear = { searchText = "" },
                onBackOrClose = {
                    if (searchActive) {
                        searchActive = false
                        if (searchText.isEmpty()) standardViewModel.onCategorySelected("New Arrivals")
                    } else {
                        onBackClick()
                    }
                },
                leadingIcon = { AnimatedSearchIcon(isSearchActive = searchActive) },
                searchContent = {
                    SearchSuggestions(
                        onSuggestionClick = { suggestion ->
                            searchText = suggestion
                            standardViewModel.onSearchQuery(suggestion)
                            searchActive = false
                            focusManager.clearFocus()
                        }
                    )
                },
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    standardViewModel.onCategorySelected(category)
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