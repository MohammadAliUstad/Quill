package com.yugentech.quill.ui.main.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.yugentech.quill.navigation.host.rememberNavTransitions
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
import androidx.compose.material.icons.rounded.Category
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.mapper.toBook
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.ui.main.components.BottomBar
import com.yugentech.quill.ui.main.components.ExitConfirmationDialog
import com.yugentech.quill.ui.main.components.LogoutConfirmationDialog
import com.yugentech.quill.ui.main.components.QuillTab
import com.yugentech.quill.ui.main.components.ResumeFab
import com.yugentech.quill.ui.main.components.ToastMessage
import com.yugentech.quill.sources.discover.viewmodel.DiscoverViewModel
import com.yugentech.quill.ui.tabs.discoverScreen.components.SubjectsSheet
import com.yugentech.quill.ui.tabs.discoverScreen.parent.DiscoverScreen
import com.yugentech.quill.ui.tabs.libraryScreen.parent.LibraryScreen
import com.yugentech.quill.ui.info.indexing.viewmodel.IndexingViewModel
import com.yugentech.quill.ui.tabs.moreScreen.parent.MoreScreen
import com.yugentech.quill.ui.main.importBooks.FilePickerBottomSheet
import com.yugentech.quill.ui.main.importBooks.ImportResult
import com.yugentech.quill.ui.main.importBooks.ImportStatusSheet
import com.yugentech.quill.ui.main.importBooks.ImportViewModel
import com.yugentech.quill.ui.shared.scrim.rememberSmoothBottomScrimBrush
import com.yugentech.quill.user.viewmodel.UserViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.localization.AppStrings
import com.yugentech.quill.announcements.ui.AnnouncementHost

private val quillTabs = listOf(
    QuillTab.Library,
    QuillTab.Discover,
    QuillTab.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLibraryBookClick: (Book) -> Unit,
    onDiscoverBookClick: (Book) -> Unit,
    onResumeClick: (Book) -> Unit,
    // A whole Discover shelf: New Arrivals, or a subject by its English name
    onShelfClick: (String) -> Unit,
    onSeeAllClick: (title: String) -> Unit,
    onAboutClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    onManageStorage: () -> Unit = {},
    onAiraSettings: () -> Unit = {},
    onViewInsights: () -> Unit = {},
    onExitApp: () -> Unit = {},
    onSignOut: () -> Unit = {},
    libraryViewModel: LibraryViewModel,
    userViewModel: UserViewModel = koinViewModel(),
    importViewModel: ImportViewModel = koinViewModel(),
    // Shared with the Discover tab (the same screen scope), so its Subjects button knows them
    discoverViewModel: DiscoverViewModel = koinViewModel(),
    onSubscriptions: () -> Unit,
    onViewIndexingQueue: () -> Unit,
    onWhatsNew: () -> Unit,
    onMoreApps: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onLanguage: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    // The signed-in account's ID or the local guest's
    userId: String,
    isGuest: Boolean = false,
    indexingViewModel: IndexingViewModel = koinViewModel()
) {
    val haptic = LocalHapticFeedback.current

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
                toastMessage = AppStrings.get(R.string.notification_permission_denied)
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
    val isIndexingActive = !queueState.isNullOrEmpty()

    var currentTab by rememberSaveable { mutableStateOf(QuillTab.Library) }
    var scrollLibraryToBottom by remember { mutableStateOf(false) }
    var isScrollingDown by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    var isLibraryEmpty by remember { mutableStateOf(false) }
    var showFilePickerSheet by remember { mutableStateOf(false) }
    val importResults by importViewModel.importResults.collectAsStateWithLifecycle()
    val isImporting by importViewModel.isImporting.collectAsStateWithLifecycle()

    val discoverState by discoverViewModel.uiState.collectAsStateWithLifecycle()
    var isDiscoverSearchOpen by remember { mutableStateOf(false) }
    var showSubjectsSheet by remember { mutableStateOf(false) }

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
                tabs = quillTabs,
                currentTab = currentTab,
                onTabSelected = { tab -> currentTab = tab },
                isImporting = isImporting,
                onImportClick = { showFilePickerSheet = true },
            )
        },
        floatingActionButton = {
            // One button per tab, in the same spot: switching tabs scales one out as the other
            // scales in. Both shrink to their icon while the page scrolls down.
            Box(contentAlignment = Alignment.BottomEnd) {
                // Empty library: Add Books, the same import as the dock's + button, so a new
                // reader's first step is spelled out. Otherwise it's Continue reading.
                ResumeFab(
                    visible = currentTab == QuillTab.Library && (isLibraryEmpty || lastReadBook != null),
                    isScrollingDown = isScrollingDown,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        if (isLibraryEmpty) {
                            showFilePickerSheet = true
                        } else {
                            lastReadBook?.let { onResumeClick(it.toBook()) }
                        }
                    },
                    icon = if (isLibraryEmpty) Icons.Default.Add else Icons.AutoMirrored.Filled.MenuBook,
                    label = if (isLibraryEmpty) stringResource(R.string.add_books) else stringResource(R.string.onboarding_continue),
                )

                // Every subject in the catalogue, once they've loaded; out of the way of the search
                ResumeFab(
                    visible = currentTab == QuillTab.Discover &&
                        discoverState.subjects.isNotEmpty() &&
                        !isDiscoverSearchOpen,
                    isScrollingDown = isScrollingDown,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        showSubjectsSheet = true
                    },
                    icon = Icons.Rounded.Category,
                    label = stringResource(R.string.discover_subjects),
                )
            }
        },
    ) { innerPadding ->
        val transitions = rememberNavTransitions()
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    // The same shared axis X as opening a screen (Hajime's tab switch), moving the
                    // way the tab bar reads
                    val navigatingRight = quillTabs.indexOf(targetState) > quillTabs.indexOf(initialState)
                    transitions.tabSwitch(forward = navigatingRight)
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
                        )

                        QuillTab.Discover -> DiscoverScreen(
                            contentPadding = innerPadding,
                            onBookClick = { book -> onDiscoverBookClick(book) },
                            onShelfClick = onShelfClick,
                            onSearchExpandedChange = { isDiscoverSearchOpen = it },
                            viewModel = discoverViewModel,
                        )

                        QuillTab.Settings -> MoreScreen(
                            contentPadding = innerPadding,
                            userData = userData,
                            streakCount = userUiState.streakCount,
                            onUpdateName = userViewModel::updateName,
                            onUpdateAvatar = userViewModel::updateAvatar,
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
                            onMoreApps = onMoreApps,
                            onReplayOnboarding = onReplayOnboarding,
                            onLanguage = onLanguage,
                            isGuest = isGuest,
                            // A guest has no account to come back to, so leaving removes their library
                            onExitGuest = onSignOut,
                        )
                    }
                }
            }

            // A soft fade into the background along the bottom, under the dock, so the page
            // dissolves away behind it instead of being cut off at a hard edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(innerPadding.calculateBottomPadding() + BottomFadeExtra)
                    .background(rememberSmoothBottomScrimBrush(MaterialTheme.colorScheme.background))
            )

            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Centralized update announcements & dialogs (shown once per release)
            AnnouncementHost(
                onOpenWhatsNew = onWhatsNew,
                onNavigateRoute = onNavigateRoute
            )
        }
    }

    if (showFilePickerSheet) {
        FilePickerBottomSheet(
            onDismiss = { showFilePickerSheet = false },
            onFilesSelected = { uris ->
                showFilePickerSheet = false
                importViewModel.importFiles(context, uris)
            },
        )
    }

    if (showSubjectsSheet) {
        SubjectsSheet(
            subjects = discoverState.subjects,
            onSubjectClick = onShelfClick,
            onDismiss = { showSubjectsSheet = false },
        )
    }

    if (importResults.isNotEmpty()) {
        ImportStatusSheet(
            results = importResults,
            onDismiss = {
                val hasSuccess = importResults.any { it is ImportResult.Success }
                importViewModel.clearResults()
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

/** How far the bottom fade reaches above the dock. */
private val BottomFadeExtra = 24.dp
