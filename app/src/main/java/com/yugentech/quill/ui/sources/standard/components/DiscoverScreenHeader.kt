package com.yugentech.quill.ui.sources.standard.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenHeader(
    searchText: String,
    searchActive: Boolean,
    dockedWidth: Dp,
    onSearchTextChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchClear: () -> Unit,
    // No back button needed on main discover, just expand/collapse logic
    onSearchExpand: () -> Unit,
    searchContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = onSearchTextChange,
                    onSearch = onSearchSubmit,
                    expanded = searchActive,
                    onExpandedChange = onSearchActiveChange,
                    placeholder = { Text("Search titles, authors...") },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (searchActive) {
                                    searchActive = false
                                    focusManager.clearFocus()
                                } else {
                                    searchActive = true
                                }
                            }
                        ) {
                            // UPDATED: Using AnimatedSearchIcon
                            AnimatedSearchIcon(isSearchActive = searchActive)
                        }
                    },
                    trailingIcon = {
                        if (searchActive && searchText.isNotEmpty()) {
                            IconButton(onClick = onSearchClear) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
            },
            expanded = searchActive,
            onExpandedChange = onSearchActiveChange,
            modifier = Modifier.widthIn(min = dockedWidth),
            // Use default colors (opaque) to match Standard Screen
            colors = SearchBarDefaults.colors(),
            windowInsets = SearchBarDefaults.windowInsets
        ) {
            searchContent()
        }
    }
}