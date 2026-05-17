package com.yugentech.quill.ui.sources.gutenberg.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GutenbergScreenHeader(
    modifier: Modifier = Modifier,
    searchText: String,
    searchActive: Boolean,
    dockedWidth: Dp,
    onSearchTextChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchClear: () -> Unit,
    onBackOrClose: () -> Unit,
    leadingIcon: @Composable () -> Unit,
    searchContent: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    // Starts completely solid
                    0.0f to surfaceColor.copy(alpha = 1.0f),
                    // Stays very strong through the first 30%
                    0.3f to surfaceColor.copy(alpha = 0.95f),
                    // Still quite prominent at the middle
                    0.6f to surfaceColor.copy(alpha = 0.80f),
                    // Quick fade out towards the bottom
                    0.85f to surfaceColor.copy(alpha = 0.30f),
                    1.0f to surfaceColor.copy(alpha = 0.0f)
                )
            )
            // --- THE CRITICAL FIX ---
            // Removes the bottom gap when the search bar is expanded so it can reach the bottom edge!
            .padding(bottom = if (searchActive) 0.dp else 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = onSearchTextChange,
                    onSearch = onSearchSubmit,
                    expanded = searchActive,
                    onExpandedChange = onSearchActiveChange,
                    placeholder = { Text("Search Gutenberg") },
                    leadingIcon = {
                        IconButton(onClick = onBackOrClose) {
                            leadingIcon()
                        }
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
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
            windowInsets = SearchBarDefaults.windowInsets
        ) {
            searchContent()
        }
    }
}