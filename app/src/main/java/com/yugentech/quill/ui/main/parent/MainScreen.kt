package com.yugentech.quill.ui.main.parent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.yugentech.quill.ui.main.components.ToastMessage
import com.yugentech.quill.ui.tabs.discoverScreen.parent.DiscoverScreen
import com.yugentech.quill.ui.tabs.libraryScreen.parent.LibraryScreen
import com.yugentech.quill.ui.info.indexing.viewmodel.IndexingViewModel
import com.yugentech.quill.ui.tabs.moreScreen.parent.MoreScreen
import com.yugentech.quill.ui.tabs.sourcesScreen.parent.SourcesScreen
import com.yugentech.quill.ui.tabs.sourcesScreen.components.FilePickerBottomSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.components.ImportStatusSheet
import com.yugentech.quill.ui.tabs.sourcesScreen.result.ImportResult
import com.yugentech.quill.ui.tabs.sourcesScreen.viewmodel.SourcesViewModel
import com.yugentech.quill.user.viewmodel.UserViewModel
import kotlinx.coroutines.delay
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
    onWhatsNew: () -> Unit,
    indexingViewModel: IndexingViewModel = koinViewModel()
) {
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) userViewModel.loadUser(userId)
    }

    val context = LocalContext.current
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var hasCheckedPermission by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                toastMessage =
                    "Notification permission denied. Please enable notifications to get your reading reminders."
            }
        }
    )

    // Android 13+ needs POST_NOTIFICATIONS granted at runtime, and notifications default to
    // enabled in settings -- so ask once on launch, otherwise reminders would be silently
    // dropped while the settings toggle still shows them as on.
    LaunchedEffect(Unit) {
        if (!hasCheckedPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                delay(500)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            hasCheckedPermission = true
        }
    }

    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val userData = userUiState.user ?: UserData()

    val queueState by indexingViewModel.queueState.collectAsStateWithLifecycle()
    val isIndexingActive = queueState.isNotEmpty()

    var currentTab by rememberSaveable { mutableStateOf(QuillTab.Library) }
    var scrollLibraryToBottom by remember { mutableStateOf(false) }
    var isScrollingDown by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    var isLibraryEmpty by remember { mutableStateOf(false) }
    var showFilePickerSheet by remember { mutableStateOf(false) }
    // Only show the import result sheet here for imports started from the Library FAB --
    // the Sources tab shows its own sheet for imports started there.
    var importStartedFromLibrary by remember { mutableStateOf(false) }
    val importResults by sourcesViewModel.importResults.collectAsStateWithLifecycle()

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
            // Empty library: the FAB becomes "Add Books" and runs the same device import flow
            // as the Sources tab. Otherwise it's the usual Continue-reading FAB.
            ResumeFab(
                visible = currentTab == QuillTab.Library && (isLibraryEmpty || lastReadBook != null),
                isScrollingDown = isScrollingDown,
                onClick = {
                    if (isLibraryEmpty) {
                        showFilePickerSheet = true
                    } else {
                        lastReadBook?.let { onResumeClick(it.toBook()) }
                    }
                },
                icon = if (isLibraryEmpty) Icons.Default.Add else Icons.AutoMirrored.Filled.MenuBook,
                label = if (isLibraryEmpty) "Add Books" else "Continue",
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
                            scrollToBottom = scrollLibraryToBottom,
                            onScrollToBottomHandled = { scrollLibraryToBottom = false },
                            onEmptyStateChange = { isLibraryEmpty = it },
                            onAddBooksClick = { showFilePickerSheet = true },
                        )

                        QuillTab.Discover -> DiscoverScreen(
                            contentPadding = innerPadding,
                            onBookClick = { book -> onDiscoverBookClick(book) },
                        )

                        QuillTab.Sources -> SourcesScreen(
                            contentPadding = innerPadding,
                            onSourceClick = onSourceClick,
                            viewModel = sourcesViewModel,
                            onLocalFilesClick = {
                                currentTab = QuillTab.Library
                                scrollLibraryToBottom = true
                            },
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
                            onWhatsNew = onWhatsNew,
                        )
                    }
                }
            }

            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showFilePickerSheet) {
        FilePickerBottomSheet(
            onDismiss = { showFilePickerSheet = false },
            onFilesSelected = { uris ->
                showFilePickerSheet = false
                importStartedFromLibrary = true
                sourcesViewModel.importFiles(context, uris)
            },
        )
    }

    if (importStartedFromLibrary && importResults.isNotEmpty()) {
        ImportStatusSheet(
            results = importResults,
            onDismiss = {
                val hasSuccess = importResults.any { it is ImportResult.Success }
                sourcesViewModel.clearResults()
                importStartedFromLibrary = false
                if (hasSuccess) {
                    currentTab = QuillTab.Library
                    scrollLibraryToBottom = true
                }
            },
        )
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