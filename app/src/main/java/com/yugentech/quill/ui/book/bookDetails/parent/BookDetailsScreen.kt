package com.yugentech.quill.ui.book.bookDetails.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.ui.book.bookDetails.components.BookDescriptionSection
import com.yugentech.quill.ui.book.bookDetails.components.SourceMaintenanceSection
import com.yugentech.quill.ui.book.bookDetails.components.BookDetailsTopBar
import com.yugentech.quill.ui.book.bookDetails.components.BookHeaderContent
import com.yugentech.quill.ui.book.bookDetails.components.BookParallaxBackground
import com.yugentech.quill.ui.book.bookDetails.components.CategorySelectionSheet
import com.yugentech.quill.ui.common.components.FloatingActionButton
import com.yugentech.quill.ui.book.bookDetails.components.ReadingProgressSection
import com.yugentech.quill.ui.book.bookDetails.components.chaptersListSection
import com.yugentech.quill.ui.book.bookDetails.components.cleanSubjects
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@OptIn(
    ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun BookDetailsScreen(
    onBackClick: () -> Unit,
    onHighlightsClick: (bookId: String) -> Unit,
    onVisualsClick: (bookId: String) -> Unit,
    onReadClick: (String, String?) -> Unit,
    onAiraClick: (String) -> Unit,
    onGoToCategoriesClick: () -> Unit,
    bookDetailsViewModel: BookDetailsViewModel,
    // Off for a guest: Aira (and her Visuals) need an account
    airaEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val uiState by bookDetailsViewModel.uiState.collectAsStateWithLifecycle()
    val categories by bookDetailsViewModel.categories.collectAsStateWithLifecycle()

    val book = uiState.book ?: return
    val chapters = uiState.chapters
    val isDescriptionExpanded = uiState.isDescriptionExpanded
    val downloadStatus = book.downloadStatus
    val currentCategory = book.userCategory ?: "Library"

    var showCategorySheet by remember { mutableStateOf(false) }
    var showRemoveLibraryWarningDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    var isScrollingDown by remember { mutableStateOf(false) }
    val fabNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) isScrollingDown = true
                else if (available.y > 10f) isScrollingDown = false
                return Offset.Zero
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > with(density) { 50.dp.toPx() }
        }
    }

    val parallaxScrollOffset by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset
            } else {
                10000
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(fabNestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BookDetailsTopBar(
                bookTitle = book.title,
                bookAuthor = book.author,
                isVisible = showTopBarTitle,
                isFavorite = book.isFavorite,
                isDownloaded = downloadStatus == DownloadStatus.DOWNLOADED,
                hasProgress = book.progressPercent > 0f,
                onBackClick = onBackClick,
                onFavoriteClick = { bookDetailsViewModel.onFavoriteToggle() },
                onDeleteClick = { showDeleteDialog = true },
                onResetProgressClick = { showResetDialog = true },
                scrollBehavior = scrollBehavior,
                onHighlightsClick = { onHighlightsClick(book.id) },
                onVisualsClick = if (airaEnabled) ({ onVisualsClick(book.id) }) else null
            )
        },
        floatingActionButton = {
            if (airaEnabled && downloadStatus == DownloadStatus.DOWNLOADED) {
                FloatingActionButton(
                    currentTabHasFab = true,
                    isScrollingDown = isScrollingDown,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onAiraClick(book.id)
                    }
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            BookParallaxBackground(
                book = book,
                scrollState = parallaxScrollOffset,
                headerHeight = 550.dp
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    bottom = innerPadding.calculateBottomPadding() + 8.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    BookHeaderContent(
                        book = book,
                        topPadding = innerPadding.calculateTopPadding(),
                        isSourceDown = uiState.isSourceDown,
                        downloadProgress = uiState.downloadProgress,
                        onCategoryClick = { showCategorySheet = true },
                        onDownloadClick = {
                            if (downloadStatus == DownloadStatus.DOWNLOADED) {
                                showDeleteDialog = true
                            } else {
                                bookDetailsViewModel.onDownloadClick()
                            }
                        },
                        onReadClick = { onReadClick(book.id, null) }
                    )
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }

                item {
                    AnimatedVisibility(
                        visible = book.progressPercent > 0f,
                        enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                                expandVertically(
                                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                                    expandFrom = Alignment.Top
                                ),
                        exit = fadeOut(tween(100, easing = FastOutLinearInEasing)) +
                               shrinkVertically(
                                   animationSpec = tween(220, delayMillis = 100, easing = FastOutSlowInEasing),
                                   shrinkTowards = Alignment.Top
                               )
                    ) {
                        Column {
                            ReadingProgressSection(
                                book = book,
                                onContinueClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    onReadClick(book.id, null)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Can't be downloaded while its source is down for maintenance: that's what the
                // page says in place of the description, as Discover's maintenance card does
                val isUnavailable = uiState.isSourceDown &&
                    downloadStatus != DownloadStatus.DOWNLOADED && downloadStatus != DownloadStatus.DOWNLOADING
                if (!uiState.isLoading && isUnavailable) {
                    item {
                        SourceMaintenanceSection(
                            source = book.source,
                            backAfter = uiState.sourceBackAfter,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else if (!uiState.isLoading) {
                    item {
                        BookDescriptionSection(
                            description = book.description,
                            subjects = book.subjects,
                            horizontalPadding = 24.dp,
                            isExpanded = isDescriptionExpanded,
                            onExpandedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                bookDetailsViewModel.onToggleDescription()
                            }
                        )
                    }
                }

                if (chapters.isNotEmpty()) {
                    // The usual space above the chapters is the gap below the subjects row. A
                    // description without that row ends on its own spacing, so none is added.
                    val descriptionShown = !uiState.isLoading && !isUnavailable
                    val endsWithoutSubjects = descriptionShown && cleanSubjects(book.subjects).isEmpty()
                    chaptersListSection(
                        topSpacing = if (endsWithoutSubjects) 0.dp else 16.dp,
                        chapters = chapters,
                        onChapterClick = { href ->
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onReadClick(book.id, href)
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }

    if (showCategorySheet) {
        CategorySelectionSheet(
            categories = categories,
            currentCategory = currentCategory,
            onDismiss = { showCategorySheet = false },
            onCategorySelected = { newCat ->
                bookDetailsViewModel.onCategoryChange(newCat)
            },
            onRemoveClick = {
                showCategorySheet = false
                if (downloadStatus == DownloadStatus.DOWNLOADED) {
                    showRemoveLibraryWarningDialog = true
                } else {
                    bookDetailsViewModel.removeFromLibrary()
                    // Only an imported book leaves: a catalogue one stays, fresh
                    if (book.source == BookSource.USER_IMPORTED) onBackClick()
                }
            },
            onGoToCategoriesClick = {
                showCategorySheet = false
                onGoToCategoriesClick()
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text(stringResource(R.string.delete_file_title)) },
            text = { Text(stringResource(R.string.delete_file_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        if (book.source == BookSource.USER_IMPORTED) {
                            bookDetailsViewModel.removeFromLibrary()
                            onBackClick()
                        } else {
                            bookDetailsViewModel.deleteBook()
                        }

                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete_file))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    showDeleteDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text(stringResource(R.string.reset_progress_title)) },
            text = { Text(stringResource(R.string.reset_progress_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        bookDetailsViewModel.resetReadingProgress()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.reset_progress))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    showResetDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showRemoveLibraryWarningDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveLibraryWarningDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text(stringResource(R.string.remove_library_title)) },
            text = { Text(stringResource(R.string.remove_library_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        bookDetailsViewModel.removeFromLibrary()
                        showRemoveLibraryWarningDialog = false
                        if (book.source == BookSource.USER_IMPORTED) onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.remove_completely))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    showRemoveLibraryWarningDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}