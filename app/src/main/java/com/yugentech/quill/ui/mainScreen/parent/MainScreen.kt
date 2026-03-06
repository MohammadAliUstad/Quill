package com.yugentech.quill.ui.mainScreen.parent

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.mapper.toBook
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.view.LibraryBookView
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.ui.mainScreen.components.BottomBar
import com.yugentech.quill.ui.mainScreen.components.QuillTab
import com.yugentech.quill.ui.mainScreen.components.ResumeFab
import com.yugentech.quill.ui.mainScreen.components.TopBar
import com.yugentech.quill.ui.tabs.discoverScreen.parent.DiscoverScreen
import com.yugentech.quill.ui.tabs.libraryScreen.parent.LibraryScreen
import com.yugentech.quill.ui.tabs.moreScreen.parent.SettingsScreen
import com.yugentech.quill.ui.tabs.sourcesScreen.parent.SourcesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLibraryBookClick: (Book) -> Unit,
    onDiscoverBookClick: (Book) -> Unit,
    onResumeClick: (Book) -> Unit,
    onSourceClick: (BookSource) -> Unit,
    onSeeAllClick: (title: String, books: List<LibraryBookView>) -> Unit,
    onAboutClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    onManageStorage: () -> Unit = {},
    libraryViewModel: LibraryViewModel,
) {
    var currentTab by rememberSaveable { mutableStateOf(QuillTab.Library) }
    var isScrollingDown by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Track scroll direction for the FAB's expand/collapse animation
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A small threshold (-5 and 5) prevents jittery animations on tiny accidental scrolls
                if (available.y < -5) {
                    isScrollingDown = true
                } else if (available.y > 5) {
                    isScrollingDown = false
                }
                return Offset.Zero
            }
        }
    }

    val lastReadBook by libraryViewModel.lastReadBook.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .nestedScroll(nestedScrollConnection),
        topBar = {
            if (currentTab != QuillTab.Discover) {
                TopBar(
                    title = currentTab.title,
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            BottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        floatingActionButton = {
            ResumeFab(
                visible = currentTab == QuillTab.Library && lastReadBook != null,
                isScrollingDown = isScrollingDown,
                onClick = { lastReadBook?.let { onResumeClick(it.toBook()) } }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                when (tab) {
                    QuillTab.Library -> LibraryScreen(
                        onLibraryBookClick = onLibraryBookClick,
                        viewModel = libraryViewModel,
                        contentPadding = innerPadding,
                        onResumeClick = onResumeClick,
                        onSeeAllClick = onSeeAllClick
                    )

                    QuillTab.Discover -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        DiscoverScreen(
                            onBookClick = { book ->
                                onDiscoverBookClick(book) // 2. FIX: Route through the new callback
                            }
                        )
                    }

                    QuillTab.Sources -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SourcesScreen(onSourceClick = onSourceClick, onLocalFilesClick = {})
                    }

                    QuillTab.Settings -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SettingsScreen(
                            onAbout = onAboutClick,
                            onAppearance = onAppearanceClick,
                            onManageCategories = onManageCategories,
                            onManageStorage = onManageStorage
                        )
                    }
                }
            }
        }
    }
}