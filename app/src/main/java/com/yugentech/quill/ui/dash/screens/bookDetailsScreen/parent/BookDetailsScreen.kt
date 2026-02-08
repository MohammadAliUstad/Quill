package com.yugentech.quill.ui.dash.screens.bookDetailsScreen.parent

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.bookDetails.BookDetailsViewModel
import com.yugentech.quill.room.entities.DownloadStatus
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components.BookDescriptionSection
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components.BookDetailsTopBar
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components.BookHeaderContent
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components.BookParallaxBackground
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components.CategorySelectionDialog
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components.ChaptersListSection

@OptIn(
    ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun BookDetailsScreen(
    onBackClick: () -> Unit,
    onReadClick: (String, String?) -> Unit,
    onManageCategoriesClick: () -> Unit,
    bookDetailsViewModel: BookDetailsViewModel
) {
    val uiState by bookDetailsViewModel.uiState.collectAsStateWithLifecycle()
    val categories by bookDetailsViewModel.categories.collectAsStateWithLifecycle()

    val book = uiState.book
    val libraryBook = uiState.libraryBook
    val chapters = uiState.chapters
    val isDescriptionExpanded = uiState.isDescriptionExpanded

    val downloadStatus = libraryBook?.downloadStatus ?: DownloadStatus.NOT_DOWNLOADED
    val isBookInLibrary = libraryBook != null

    var showCategoryDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val showTopBarTitle by remember {
        derivedStateOf { scrollState.value > with(density) { 50.dp.toPx() } }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BookDetailsTopBar(
                bookTitle = book.title,
                bookAuthor = book.author,
                isVisible = showTopBarTitle,
                isFavorite = isBookInLibrary,
                onBackClick = onBackClick,
                onFavoriteClick = {
                    if (!isBookInLibrary) bookDetailsViewModel.onDownloadClick()
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->

        val headerHeight = 272.dp + innerPadding.calculateTopPadding()

        Box(modifier = Modifier.fillMaxSize()) {
            BookParallaxBackground(
                book = book,
                scrollState = scrollState,
                headerHeight = headerHeight
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BookHeaderContent(
                    book = book,
                    topPadding = innerPadding.calculateTopPadding(),
                    libraryBook = libraryBook,
                    downloadStatus = downloadStatus,
                    onCategoryClick = { showCategoryDialog = true },
                    onDownloadClick = {
                        if (downloadStatus == DownloadStatus.DOWNLOADED) {
                            bookDetailsViewModel.onRemoveDownloadClick()
                        } else {
                            bookDetailsViewModel.onDownloadClick()
                        }
                    },
                    onReadClick = { onReadClick(book.id, null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // FIX: Only render the description once loading is complete.
                // This prevents the "Bounce" because the correct Expanded/Collapsed
                // state is already calculated by the ViewModel before this appears.
                if (!uiState.isLoading) {
                    BookDescriptionSection(
                        description = book.description,
                        subjects = book.subjects,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        isExpanded = isDescriptionExpanded,
                        onExpandedChange = { bookDetailsViewModel.onToggleDescription() }
                    )
                }

                if (chapters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ChaptersListSection(
                        chapters = chapters,
                        onChapterClick = { href -> onReadClick(book.id, href) },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = categories,
            currentCategory = libraryBook?.userCategory ?: "Library",
            onDismiss = { showCategoryDialog = false },
            onCategorySelected = { newCat ->
                bookDetailsViewModel.onCategoryChange(newCat)
                showCategoryDialog = false
            },
            onManageClick = {
                showCategoryDialog = false
                onManageCategoriesClick()
            }
        )
    }
}