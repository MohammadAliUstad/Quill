package com.yugentech.quill.ui.sources.standard.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import getTitleForCategory
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardScreenHeader(
    searchText: String,
    searchActive: Boolean,
    dockedWidth: Dp,
    onSearchTextChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchClear: () -> Unit,
    onBackOrClose: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = onSearchTextChange,
                    onSearch = onSearchSubmit,
                    expanded = searchActive,
                    onExpandedChange = onSearchActiveChange,
                    placeholder = { Text(stringResource(R.string.search_standard)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBackOrClose()
                        }) {
                            AnimatedSearchIcon(isSearchActive = searchActive)
                        }
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                onSearchClear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                )
            },
            expanded = searchActive,
            onExpandedChange = onSearchActiveChange,
            modifier = Modifier.widthIn(min = dockedWidth),
            windowInsets = SearchBarDefaults.windowInsets
        ) {
            if (searchText.isEmpty()) {
                StandardSearchPrompt()
            }
        }

        // The chips fade in together once the category cache answers (nothing until then, rather
        // than New Arrivals alone). The row keeps a chip's height while empty, so the header
        // doesn't grow under the search bar. Each chip is keyed, so a refresh that adds or drops
        // a subject animates just that chip instead of the row jumping.
        val rowAlpha by animateFloatAsState(
            targetValue = if (categories.isEmpty()) 0f else 1f,
            animationSpec = tween(220),
            label = "categoryRowAlpha"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp)
                .heightIn(min = FilterChipDefaults.Height)
                .graphicsLayer { alpha = rowAlpha }
        ) {
            items(categories, key = { it }) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    modifier = Modifier.animateItem(),
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onCategorySelected(category)
                    },
                    label = { Text(getTitleForCategory(category)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}