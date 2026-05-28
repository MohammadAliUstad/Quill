package com.yugentech.quill.ui.sources.gutenberg.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
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
    onBackOrClose: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    val animatedContainerColor by animateColorAsState(
        targetValue = if (searchActive) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        },
        label = "container_color"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (searchActive) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        label = "border_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to surfaceColor.copy(alpha = 1.0f),
                    0.3f to surfaceColor.copy(alpha = 0.95f),
                    0.6f to surfaceColor.copy(alpha = 0.80f),
                    0.85f to surfaceColor.copy(alpha = 0.30f),
                    1.0f to surfaceColor.copy(alpha = 0.0f)
                )
            )
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
                            com.yugentech.quill.ui.sources.standard.components.AnimatedSearchIcon(isSearchActive = searchActive)
                        }
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = onSearchClear) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = animatedBorderColor,
                        shape = RoundedCornerShape(28.dp)
                    )
                )
            },
            expanded = searchActive,
            onExpandedChange = onSearchActiveChange,
            modifier = Modifier.widthIn(min = dockedWidth),
            colors = SearchBarDefaults.colors(
                containerColor = animatedContainerColor,
                dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            windowInsets = SearchBarDefaults.windowInsets
        ) {
            if (searchText.isEmpty()) {
                SearchPrompt()
            }
        }
    }
}
