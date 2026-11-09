package com.yugentech.quill.ui.tabs.moreScreen.parent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.ui.about.about.parent.MoreFromUsSheet
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.quill.ui.shared.scrim.rememberSmoothScrimBrush
import com.yugentech.quill.ui.tabs.moreScreen.components.AvatarPickerSheet
import com.yugentech.quill.ui.tabs.moreScreen.components.EditNameSheet
import com.yugentech.quill.ui.tabs.moreScreen.components.ProfileCard
import com.yugentech.quill.ui.tabs.moreScreen.components.SettingsListItem
import com.yugentech.quill.ui.tabs.moreScreen.components.SettingsSwitchItem
import com.yugentech.quill.ui.tabs.moreScreen.components.dialogs.AlarmPermissionDialog
import com.yugentech.quill.ui.tabs.moreScreen.components.dialogs.TimerPickerDialog
import com.yugentech.quill.notification.viewmodel.NotificationViewModel
import com.yugentech.quill.ui.tabs.moreScreen.viewmodel.SettingsViewModel
import com.yugentech.theme.tokens.spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    userData: UserData,
    streakCount: Int,
    isIndexingActive: Boolean,
    onSignOut: () -> Unit,
    onExit: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateAvatar: (Int) -> Unit,
    onViewInsights: () -> Unit,
    onAbout: () -> Unit,
    onAppearance: () -> Unit,
    onManageCategories: () -> Unit,
    onManageStorage: () -> Unit,
    onAboutAira: () -> Unit,
    onSubscriptions: () -> Unit,
    onViewIndexingQueue: () -> Unit,
    onWhatsNew: () -> Unit,
    onMoreApps: () -> Unit,
    onReplayOnboarding: () -> Unit,
    notificationViewModel: NotificationViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val notificationConfig by notificationViewModel.notificationConfig.collectAsState()
    val showPermissionDialog by notificationViewModel.showExactAlarmDialog.collectAsStateWithLifecycle()
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState(initial = true)

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showMoreApps by rememberSaveable { mutableStateOf(false) }
    // The name and avatar are edited in sheets right here (there's no Edit Profile screen).
    var showNameSheet by rememberSaveable { mutableStateOf(false) }
    var showAvatarSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Returning from the exact-alarm settings screen with the permission granted picks up
    // where the user left off (opens the time picker) instead of needing another tap.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (notificationViewModel.onReturnedFromSettings()) {
            showTimePickerDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationViewModel.setNotificationsEnabled(true)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val surfaceColor = MaterialTheme.colorScheme.surface

            // Same as Library: a transparent bar over a gradient scrim that fades in as the
            // list scrolls under it
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = if (listState.firstVisibleItemIndex > 0) {
                                1f
                            } else {
                                (listState.firstVisibleItemScrollOffset / 100f).coerceIn(0f, 1f)
                            }
                        }
                        .background(rememberSmoothScrimBrush(surfaceColor))
                )

                TopAppBar(
                    title = {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.m,
                end = MaterialTheme.spacing.m,
                top = innerPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
        ) {
            item {
                ProfileCard(
                    userData = userData,
                    streakCount = streakCount,
                    onEditName = { showNameSheet = true },
                    onEditAvatar = { showAvatarSheet = true },
                    onViewInsights = onViewInsights
                )
            }

            // --- AI SECTION ---
            val aiItemCount = if (isIndexingActive) 3 else 2
            item { SectionHeader(icon = Icons.Default.AutoAwesome, title = "AI") }
            item {
                SettingsListItem(
                    title = "Meet Aira",
                    subtitle = "Learn about your AI reading companion",
                    index = 0,
                    totalCount = aiItemCount,
                    onClick = onAboutAira,
                )
            }
            item {
                SettingsListItem(
                    title = "Subscriptions",
                    subtitle = "Manage your plan and unlock pro features",
                    index = 1,
                    totalCount = aiItemCount,
                    onClick = onSubscriptions,
                )
            }
            if (isIndexingActive) {
                item(key = "indexing_queue") {
                    SettingsListItem(
                        modifier = Modifier.animateItem(),
                        title = "Indexing Library",
                        subtitle = "Aira is reading your books.",
                        index = 2,
                        totalCount = aiItemCount,
                        onClick = onViewIndexingQueue,
                    )
                }
            }

            // --- LIBRARY SECTION ---
            item { SectionHeader(icon = Icons.AutoMirrored.Filled.List, title = "Library") }
            item {
                SettingsListItem(
                    title = "Manage Categories",
                    subtitle = "Add, remove, or reorder your bookshelves",
                    index = 0,
                    totalCount = 2,
                    onClick = onManageCategories,
                )
            }
            item {
                SettingsListItem(
                    title = "Manage Storage",
                    subtitle = "View device storage and remove downloads",
                    index = 1,
                    totalCount = 2,
                    onClick = onManageStorage,
                )
            }

            // --- APPEARANCE SECTION ---
            item { SectionHeader(icon = Icons.Default.Palette, title = "Appearance") }
            item {
                SettingsListItem(
                    title = "Theme & Colors",
                    subtitle = "Customize your app's look and feel",
                    index = 0,
                    totalCount = 1,
                    onClick = onAppearance,
                )
            }

            // --- NOTIFICATIONS & HAPTICS SECTION ---
            item { SectionHeader(icon = Icons.Default.Notifications, title = "Notifications & Haptics") }
            item {
                SettingsSwitchItem(
                    title = "Enable Notifications",
                    subtitle = "Allow Quill to send you updates",
                    checked = notificationConfig.notificationsEnabled,
                    checkedIcon = Icons.Default.Notifications,
                    index = 0,
                    totalCount = 4,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationViewModel.setNotificationsEnabled(true)
                            }
                        } else {
                            notificationViewModel.setNotificationsEnabled(enabled)
                        }
                        haptic.performHapticFeedback(if (enabled) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Daily Reading Reminder",
                    subtitle = notificationViewModel.formatReminderTime(),
                    checked = notificationConfig.dailyReminderEnabled,
                    checkedIcon = Icons.Default.Alarm,
                    enabled = notificationConfig.notificationsEnabled,
                    index = 1,
                    totalCount = 4,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (notificationViewModel.canEnableReminders()) {
                                showTimePickerDialog = true
                            }
                        } else {
                            notificationViewModel.setDailyReminderEnabled(false)
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        }
                    },
                    onClick = {
                        if (notificationConfig.notificationsEnabled && notificationViewModel.canEnableReminders()) {
                            showTimePickerDialog = true
                        }
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Playful Reminders",
                    subtitle = "Friendly nudges now and then",
                    checked = notificationConfig.playfulRemindersEnabled,
                    checkedIcon = Icons.Default.AutoAwesome,
                    enabled = notificationConfig.notificationsEnabled,
                    index = 2,
                    totalCount = 4,
                    onCheckedChange = {
                        notificationViewModel.setPlayfulRemindersEnabled(it)
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Haptic Feedback",
                    subtitle = "Feel vibrations throughout the app",
                    checked = hapticsEnabled,
                    checkedIcon = Icons.Default.Vibration,
                    index = 3,
                    totalCount = 4,
                    onCheckedChange = {
                        settingsViewModel.setHapticsEnabled(it)
                        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                    }
                )
            }

            // --- ABOUT SECTION ---
            item { SectionHeader(icon = Icons.Default.Info, title = "About") }
            item {
                SettingsListItem(
                    title = "About Quill",
                    subtitle = "Version ${com.yugentech.quill.BuildConfig.VERSION_NAME}",
                    index = 0,
                    totalCount = 4,
                    onClick = onAbout,
                )
            }
            item {
                SettingsListItem(
                    title = "What's New",
                    subtitle = "See the latest changes and features",
                    index = 1,
                    totalCount = 4,
                    onClick = onWhatsNew,
                )
            }
            item {
                SettingsListItem(
                    title = "Replay Onboarding",
                    subtitle = "Take the tour of Quill again",
                    index = 2,
                    totalCount = 4,
                    onClick = onReplayOnboarding,
                )
            }
            item {
                SettingsListItem(
                    title = "More from us",
                    subtitle = "Explore our other apps",
                    index = 3,
                    totalCount = 4,
                    onClick = { showMoreApps = true },
                )
            }

            // --- SESSION SECTION ---
            item { SectionHeader(icon = Icons.AutoMirrored.Filled.ExitToApp, title = "Session") }
            item {
                SettingsListItem(
                    title = "Exit",
                    subtitle = "Close the Quill app",
                    index = 0,
                    totalCount = 2,
                    onClick = onExit,
                )
            }
            item {
                SettingsListItem(
                    title = "Sign Out",
                    subtitle = "Log out of your current account",
                    index = 1,
                    totalCount = 2,
                    onClick = onSignOut,
                )
            }
        }
    }

    if (showTimePickerDialog) {
        TimerPickerDialog(
            initialHour = notificationConfig.reminderHour,
            initialMinute = notificationConfig.reminderMinute,
            onTimeSelected = { hour, minute ->
                notificationViewModel.setReminderTime(hour, minute)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    if (showNameSheet && userData.userId.isNotEmpty()) {
        EditNameSheet(
            currentName = userData.name.orEmpty(),
            onSave = { name ->
                onUpdateName(name)
                showNameSheet = false
            },
            onDismiss = { showNameSheet = false }
        )
    }

    if (showAvatarSheet && userData.userId.isNotEmpty()) {
        AvatarPickerSheet(
            selectedAvatarId = userData.avatarId ?: 0,
            onAvatarSelected = onUpdateAvatar,
            onDismiss = { showAvatarSheet = false }
        )
    }

    if (showMoreApps) {
        MoreFromUsSheet(
            onDismiss = { showMoreApps = false },
            onSeeAll = onMoreApps
        )
    }

    if (showPermissionDialog) {
        AlarmPermissionDialog(
            context = context,
            onDismiss = { notificationViewModel.cancelPermissionRequest() },
            onConfirm = { notificationViewModel.dismissPermissionDialog() }
        )
    }
}
