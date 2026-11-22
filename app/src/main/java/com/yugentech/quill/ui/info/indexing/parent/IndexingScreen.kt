package com.yugentech.quill.ui.info.indexing.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.R
import com.yugentech.quill.ui.info.indexing.components.EmptyQueueState
import com.yugentech.quill.ui.info.indexing.components.QueueGridItem
import com.yugentech.quill.ui.info.indexing.state.IndexingUiState
import com.yugentech.quill.ui.info.indexing.viewmodel.IndexingViewModel
import com.yugentech.theme.tokens.dimensions.AppAnimations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexingQueueScreen(
    viewModel: IndexingViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val queue by viewModel.queueState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.reading_queue),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.reading_queue_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Nothing until the queue has loaded; then the grid, and when the last book is done
            // the grid fades out as the all-done state fades in (and back, if a new one starts)
            val current = queue
            val content = when {
                current == null -> QueueContent.Loading
                current.isEmpty() -> QueueContent.Empty
                else -> QueueContent.Books
            }
            // The last books shown, so the grid fades out with them instead of emptying first
            var lastBooks by remember { mutableStateOf<List<IndexingUiState>>(emptyList()) }
            val books = current?.takeIf { it.isNotEmpty() } ?: lastBooks
            SideEffect { if (!current.isNullOrEmpty()) lastBooks = current }
            AnimatedContent(
                targetState = content,
                transitionSpec = {
                    fadeIn(tween(AppAnimations.Durations.Standard)) togetherWith
                        fadeOut(tween(AppAnimations.Durations.Standard))
                },
                label = "indexingQueueContent",
                modifier = Modifier.fillMaxSize()
            ) { state ->
                when (state) {
                    QueueContent.Loading -> Box(Modifier.fillMaxSize())
                    QueueContent.Empty -> EmptyQueueState()
                    QueueContent.Books -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(books, key = { it.bookId }) { item ->
                            QueueGridItem(
                                modifier = Modifier.animateItem(),
                                item = item
                            )
                        }
                    }
                }
            }
        }
    }
}

/** What the queue screen shows. */
private enum class QueueContent { Loading, Empty, Books }
