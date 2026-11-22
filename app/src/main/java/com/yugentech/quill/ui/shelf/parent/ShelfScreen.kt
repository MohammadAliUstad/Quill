package com.yugentech.quill.ui.shelf.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.R
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.sources.discover.viewmodel.NEW_ARRIVALS
import com.yugentech.quill.sources.discover.viewmodel.ShelfNavigationEvent
import com.yugentech.quill.sources.discover.viewmodel.ShelfViewModel
import com.yugentech.quill.ui.common.util.ObserveAsEvents
import com.yugentech.quill.ui.common.components.rememberSmoothScrimBrush
import com.yugentech.quill.ui.common.books.BooksGrid
import com.yugentech.quill.ui.common.books.STANDARD_MIN_CELL
import com.yugentech.quill.ui.shelf.components.SourceContentHost
import com.yugentech.quill.ui.common.util.getTitleForCategory

/**
 * A whole Discover shelf as a grid: New Arrivals, or every book in one subject. Laid out like the
 * library's See all, a back arrow and the shelf's name over the covers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    onBackClick: () -> Unit,
    onNavigateById: (String) -> Unit,
    onNavigateByContent: (Book) -> Unit,
    viewModel: ShelfViewModel
) {
    val haptic = LocalHapticFeedback.current
    val content by viewModel.content.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.navigationEvents) { event ->
        when (event) {
            is ShelfNavigationEvent.NavigateById -> onNavigateById(event.id)
            is ShelfNavigationEvent.NavigateByContent -> onNavigateByContent(event.book)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                // The covers scroll up under a clear bar, which frosts over once they do
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = (-scrollBehavior.state.contentOffset / 100f).coerceIn(0f, 1f)
                        }
                        .background(rememberSmoothScrimBrush(surfaceColor))
                )

                TopAppBar(
                    title = {
                        Text(
                            // New Arrivals goes by the name Discover gives it above the carousel
                            text = if (viewModel.shelf == NEW_ARRIVALS) stringResource(R.string.new_releases)
                            else getTitleForCategory(viewModel.shelf),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBackClick()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        val topPadding = innerPadding.calculateTopPadding() + 8.dp
        SourceContentHost(
            content = content,
            skeletonColumns = GridCells.Adaptive(minSize = STANDARD_MIN_CELL),
            topPadding = topPadding,
            bottomPadding = bottomPadding
        ) { books ->
            BooksGrid(
                books = books,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                onBookClick = viewModel::onBookClick
            )
        }
    }
}
