package com.yugentech.quill.ui.dash.screens.standardScreen.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.yugentech.quill.network.domain.Book
import com.yugentech.quill.ui.dash.screens.standardScreen.components.AnimatedSearchIcon
import com.yugentech.quill.ui.dash.screens.standardScreen.components.BooksGridContent
import com.yugentech.quill.ui.dash.screens.standardScreen.components.CategoryFilterRow
import com.yugentech.quill.ui.dash.screens.standardScreen.components.SearchSuggestions
import com.yugentech.quill.viewModels.StandardViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardScreen(
    onBackClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    standardViewModel: StandardViewModel = koinViewModel()
) {
    val books by standardViewModel.booksState.collectAsState()
    val isLoading by standardViewModel.isLoading.collectAsState()
    val selectedCategory by standardViewModel.selectedCategory.collectAsState()

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val searchBarHeight = 56.dp
    val searchBarTopPadding = 8.dp
    val componentSpacing = 16.dp

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dockedWidth = screenWidth - 32.dp

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BackHandler(enabled = searchActive) {
        searchActive = false
        if (searchText.isEmpty()) {
            standardViewModel.onCategorySelected("New Arrivals")
        }
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
            // --- LAYER 1: MAIN CONTENT ---
            Column(modifier = Modifier.fillMaxSize()) {
                // SPACER: Offset for content
                val headerOffset =
                    statusBarHeight + searchBarTopPadding + searchBarHeight + componentSpacing
                Spacer(modifier = Modifier.height(headerOffset))

                CategoryFilterRow(
                    categories = standardViewModel.categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { category ->
                        standardViewModel.onCategorySelected(category)
                        if (searchText.isNotEmpty()) searchText = ""
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    BooksGridContent(
                        books = books,
                        isLoading = isLoading,
                        onBookClick = onBookClick
                    )
                }
            }

            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        onQueryChange = { searchText = it },
                        onSearch = { query ->
                            standardViewModel.onSearchQuery(query)
                            searchActive = false
                            focusManager.clearFocus()
                        },
                        expanded = searchActive,
                        onExpandedChange = { searchActive = it },
                        placeholder = { Text("Search Standard Ebooks") },
                        leadingIcon = {
                            IconButton(onClick = {
                                if (searchActive) {
                                    searchActive = false
                                    if (searchText.isEmpty()) {
                                        standardViewModel.onCategorySelected("New Arrivals")
                                    }
                                } else {
                                    onBackClick()
                                }
                            }) {
                                // REPLACE THE OLD ICON WITH THIS:
                                AnimatedSearchIcon(
                                    isSearchActive = searchActive
                                )
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
                    .widthIn(min = dockedWidth),
                windowInsets = SearchBarDefaults.windowInsets
            ) {
                SearchSuggestions(
                    onSuggestionClick = { suggestion ->
                        searchText = suggestion
                        standardViewModel.onSearchQuery(suggestion)
                        searchActive = false
                        focusManager.clearFocus()
                    }
                )
            }
        }
    }
}