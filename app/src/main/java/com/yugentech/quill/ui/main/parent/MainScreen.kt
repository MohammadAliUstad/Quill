package com.yugentech.quill.ui.main.parent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.yugentech.quill.database.mapper.toBook
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.ui.main.components.BottomBar
import com.yugentech.quill.ui.main.components.ExitConfirmationDialog
import com.yugentech.quill.ui.main.components.LogoutConfirmationDialog
import com.yugentech.quill.ui.main.components.QuillTab
import com.yugentech.quill.ui.main.components.ResumeFab
import com.yugentech.quill.ui.tabs.discoverScreen.parent.DiscoverScreen
import com.yugentech.quill.ui.tabs.libraryScreen.parent.LibraryScreen
import com.yugentech.quill.ui.info.indexing.viewmodel.IndexingViewModel
import com.yugentech.quill.ui.tabs.moreScreen.parent.MoreScreen
import com.yugentech.quill.ui.tabs.sourcesScreen.parent.SourcesScreen
import com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel.SourcesViewModel
import com.yugentech.quill.user.viewmodel.UserViewModel
import org.koin.androidx.compose.koinViewModel

private val quillTabs = listOf(
    QuillTab.Library,
    QuillTab.Discover,
    QuillTab.Sources,
    QuillTab.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLibraryBookClick: (Book) -> Unit,
    onDiscoverBookClick: (Book) -> Unit,
    onResumeClick: (Book) -> Unit,
    onSourceClick: (BookSource) -> Unit,
    onSeeAllClick: (title: String) -> Unit,
    onAboutClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    onManageStorage: () -> Unit = {},
    onAiraSettings: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onViewInsights: () -> Unit = {},
    onExitApp: () -> Unit = {},
    onSignOut: () -> Unit = {},
    libraryViewModel: LibraryViewModel,
    userViewModel: UserViewModel = koinViewModel(),
    sourcesViewModel: SourcesViewModel = koinViewModel(),
    onSubscriptions: () -> Unit,
    onViewIndexingQueue: () -> Unit,
    indexingViewModel: IndexingViewModel = koinViewModel()
) {
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) userViewModel.loadUser(userId)
    }

    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val userData = userUiState.user ?: UserData()

    val queueState by indexingViewModel.queueState.collectAsStateWithLifecycle()
    val isIndexingActive = queueState.isNotEmpty()

    var currentTab by rememberSaveable { mutableStateOf(QuillTab.Library) }
    var isScrollingDown by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val saveableStateHolder = rememberSaveableStateHolder()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5) isScrollingDown = true
                else if (available.y > 5) isScrollingDown = false
                return Offset.Zero
            }
        }
    }

    val lastReadBook by libraryViewModel.lastReadBook.collectAsStateWithLifecycle()

    BackHandler(enabled = currentTab != QuillTab.Library) {
        currentTab = QuillTab.Library
    }

    BackHandler(enabled = currentTab == QuillTab.Library) {
        showExitDialog = true
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            BottomBar(
                currentTab = currentTab,
                onTabSelected = { tab -> currentTab = tab },
            )
        },
        floatingActionButton = {
            ResumeFab(
                visible = currentTab == QuillTab.Library && lastReadBook != null,
                isScrollingDown = isScrollingDown,
                onClick = { lastReadBook?.let { onResumeClick(it.toBook()) } },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val targetIndex = quillTabs.indexOf(targetState)
                    val initialIndex = quillTabs.indexOf(initialState)
                    val navigatingRight = targetIndex > initialIndex

                    if (navigatingRight) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "TabSwitch",
            ) { tab ->
                saveableStateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        QuillTab.Library -> LibraryScreen(
                            contentPadding = innerPadding,
                            onLibraryBookClick = onLibraryBookClick,
                            viewModel = libraryViewModel,
                            onResumeClick = onResumeClick,
                            onSeeAllClick = onSeeAllClick,
                        )

                        QuillTab.Discover -> DiscoverScreen(
                            contentPadding = innerPadding,
                            onBookClick = { book -> onDiscoverBookClick(book) },
                        )

                        QuillTab.Sources -> SourcesScreen(
                            contentPadding = innerPadding,
                            onSourceClick = onSourceClick,
                            viewModel = sourcesViewModel,
                            onLocalFilesClick = {},
                        )

                        QuillTab.Settings -> MoreScreen(
                            contentPadding = innerPadding,
                            userData = userData,
                            streakCount = userUiState.streakCount,
                            onEditProfile = onEditProfile,
                            onViewInsights = onViewInsights,
                            onAbout = onAboutClick,
                            onAppearance = onAppearanceClick,
                            onManageCategories = onManageCategories,
                            onManageStorage = onManageStorage,
                            onAboutAira = onAiraSettings,
                            onSignOut = { showSignOutDialog = true },
                            onSubscriptions = onSubscriptions,
                            onExit = { showExitDialog = true },
                            isIndexingActive = isIndexingActive,
                            onViewIndexingQueue = onViewIndexingQueue,
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitDialog = false
                onExitApp()
            },
            onDismiss = { showExitDialog = false },
        )
    }

    if (showSignOutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showSignOutDialog = false
                onSignOut()
            },
            onDismiss = { showSignOutDialog = false },
        )
    }
}