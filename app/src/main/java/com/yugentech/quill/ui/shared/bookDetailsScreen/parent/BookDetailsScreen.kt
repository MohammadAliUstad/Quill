package com.yugentech.quill.ui.shared.bookDetailsScreen.parent

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.FloatingActionButton
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.BookDescriptionSection
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.BookDetailsTopBar
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.BookHeaderContent
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.BookParallaxBackground
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.CategorySelectionDialog
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.ReadingProgressSection
import com.yugentech.quill.ui.shared.bookDetailsScreen.components.chaptersListSection

@OptIn(
    ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun BookDetailsScreen(
    onBackClick: () -> Unit,
    onReadClick: (String, String?) -> Unit,
    onAiraClick: (String) -> Unit,
    bookDetailsViewModel: BookDetailsViewModel
) {
    val uiState by bookDetailsViewModel.uiState.collectAsStateWithLifecycle()
    val categories by bookDetailsViewModel.categories.collectAsStateWithLifecycle()

    val book = uiState.book ?: return
    val isGutenberg = book.source == BookSource.GUTENBERG
    val chapters = uiState.chapters
    val isDescriptionExpanded = uiState.isDescriptionExpanded
    val downloadStatus = book.downloadStatus
    val currentCategory = book.userCategory ?: "Library"

    var showCategoryDialog by remember { mutableStateOf(false) }
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

    // 1. Replaced ScrollState with LazyListState
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    // 2. Updated TopBar trigger logic for LazyColumn
    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > with(density) { 50.dp.toPx() }
        }
    }

    // 3. Extracted integer scroll offset to feed into your Parallax background safely
    val parallaxScrollOffset by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset
            } else {
                10000 // Safely off-screen so the parallax stops calculating
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(fabNestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BookDetailsTopBar(
                bookTitle = book.title,
                bookAuthor = book.author,
                isVisible = showTopBarTitle,
                isFavorite = book.isFavorite,
                onBackClick = onBackClick,
                onFavoriteClick = { bookDetailsViewModel.onFavoriteToggle() },
                onDeleteClick = { showDeleteDialog = true },
                onResetProgressClick = { showResetDialog = true },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (downloadStatus == DownloadStatus.DOWNLOADED && uiState.isPro) {
                Box(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                    FloatingActionButton(
                        currentTabHasFab = true,
                        isScrollingDown = isScrollingDown,
                        onClick = { onAiraClick(book.id) }
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {

            // Note: We pass the extracted integer offset here now
            BookParallaxBackground(
                book = book,
                scrollState = parallaxScrollOffset,
                headerHeight = 550.dp
            )

            // 4. Converted Column to LazyColumn
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    BookHeaderContent(
                        book = book,
                        topPadding = innerPadding.calculateTopPadding(),
                        onCategoryClick = { showCategoryDialog = true },
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

                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (book.progressPercent > 0f) {
                    item {
                        ReadingProgressSection(
                            book = book,
                            onContinueClick = { onReadClick(book.id, null) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                if (!uiState.isLoading) {
                    item {
                        BookDescriptionSection(
                            description = book.description,
                            subjects = book.subjects,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            isExpanded = isDescriptionExpanded,
                            onExpandedChange = { bookDetailsViewModel.onToggleDescription() },
                            isGutenberg = isGutenberg
                        )
                    }
                }

                // 5. Deconstructed ChaptersListSection into Lazy rendering
                if (chapters.isNotEmpty()) {
                    chaptersListSection(
                        chapters = chapters,
                        onChapterClick = { href -> onReadClick(book.id, href) },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }

    // --- DIALOGS (Unchanged) ---
    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = categories,
            currentCategory = currentCategory,
            onDismiss = { showCategoryDialog = false },
            onCategorySelected = { newCat ->
                bookDetailsViewModel.onCategoryChange(newCat)
                showCategoryDialog = false
            },
            onRemoveClick = {
                showCategoryDialog = false
                if (downloadStatus == DownloadStatus.DOWNLOADED) {
                    showRemoveLibraryWarningDialog = true
                } else {
                    bookDetailsViewModel.removeFromLibrary()
                    onBackClick()
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Delete Book File?") },
            text = { Text("The downloaded book file will be deleted, but your reading progress and bookmarks will be kept completely safe.") },
            confirmButton = {
                TextButton(onClick = {
                    bookDetailsViewModel.deleteBook()
                    showDeleteDialog = false
                }) {
                    Text("Delete File", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Reset Reading Progress?") },
            text = { Text("Are you sure you want to reset your reading progress? All your current reading stats for this book will be permanently cleared.") },
            confirmButton = {
                TextButton(onClick = {
                    bookDetailsViewModel.resetReadingProgress()
                    showResetDialog = false
                }) {
                    Text("Reset Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRemoveLibraryWarningDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveLibraryWarningDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Remove from Library?") },
            text = { Text("This book is currently downloaded. Removing it from your library will also delete the downloaded file and permanently clear all its data.") },
            confirmButton = {
                TextButton(onClick = {
                    bookDetailsViewModel.removeFromLibrary()
                    showRemoveLibraryWarningDialog = false
                    onBackClick()
                }) {
                    Text("Remove Completely", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveLibraryWarningDialog = false }) { Text("Cancel") }
            }
        )
    }
}