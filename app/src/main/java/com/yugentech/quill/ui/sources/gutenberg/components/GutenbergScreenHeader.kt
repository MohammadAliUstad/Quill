package com.yugentech.quill.ui.sources.gutenberg.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.shared.scrim.rememberSmoothScrimBrush
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GutenbergScreenHeader(
    modifier: Modifier = Modifier,
    searchText: String,
    searchActive: Boolean,
    dockedWidth: Dp,
    hazeState: HazeState,
    statusBarHeight: Dp,
    onSearchTextChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchClear: () -> Unit,
    onBackOrClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Faded in and out from a see-through copy of the same colour: fading from
    // Color.Transparent (see-through black) passes through dark grey, a flash in light mode
    val animatedContainerColor by animateColorAsState(
        targetValue = if (searchActive) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f)
        },
        label = "container_color"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (searchActive) {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        label = "border_color"
    )

    val hazeAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (searchActive) 0f else 1f,
        label = "haze_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(rememberSmoothScrimBrush(surfaceColor))
            .padding(bottom = if (searchActive) 0.dp else 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {


        SearchBar(
            modifier = Modifier.widthIn(min = dockedWidth),
            colors = SearchBarDefaults.colors(
                containerColor = animatedContainerColor,
                dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            expanded = searchActive,
            onExpandedChange = onSearchActiveChange,
            windowInsets = SearchBarDefaults.windowInsets,
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchText,
                    onQueryChange = onSearchTextChange,
                    onSearch = onSearchSubmit,
                    expanded = searchActive,
                    onExpandedChange = onSearchActiveChange,
                    placeholder = { Text(stringResource(R.string.search_gutenberg)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBackOrClose()
                        }) {
                            com.yugentech.quill.ui.sources.standard.components.AnimatedSearchIcon(isSearchActive = searchActive)
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
                    // The frosted backdrop fades out as the bar's own fill fades in, rather than
                    // vanishing on the first frame (a see-through flicker on tap).
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeDefaults.style(
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                                blurRadius = 20.dp,
                                noiseFactor = 0.05f
                            )
                        ) { alpha = hazeAlpha }
                        .border(
                            width = 1.dp,
                            color = animatedBorderColor,
                            shape = RoundedCornerShape(28.dp)
                        )
                )
            },
        ) {
            if (searchText.isEmpty()) {
                SearchPrompt()
            }
        }
    }
}
