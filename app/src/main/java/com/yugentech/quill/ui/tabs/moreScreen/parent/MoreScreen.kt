package com.yugentech.quill.ui.tabs.moreScreen.parent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.quill.ui.tabs.moreScreen.components.ProfileCard
import com.yugentech.quill.ui.tabs.moreScreen.components.SettingsListItem
import com.yugentech.quill.ui.tabs.moreScreen.components.SettingsSwitchItem
import com.yugentech.quill.ui.tabs.moreScreen.components.dialogs.AlarmPermissionDialog
import com.yugentech.quill.ui.tabs.moreScreen.components.dialogs.TimerPickerDialog
import com.yugentech.quill.ui.tabs.moreScreen.viewmodel.NotificationViewModel
import com.yugentech.quill.ui.tabs.moreScreen.viewmodel.SettingsViewModel
import com.yugentech.theme.service.HapticService
import com.yugentech.theme.tokens.spacing
import org.koin.compose.koinInject
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
    onEditProfile: () -> Unit,
    onViewInsights: () -> Unit,
    onAbout: () -> Unit,
    onAppearance: () -> Unit,
    onManageCategories: () -> Unit,
    onManageStorage: () -> Unit,
    onAboutAira: () -> Unit,
    onSubscriptions: () -> Unit,
    onViewIndexingQueue: () -> Unit,
    onWhatsNew: () -> Unit,
    notificationViewModel: NotificationViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val notificationConfig by notificationViewModel.notificationConfig.collectAsState()
    val showPermissionDialog by notificationViewModel.showExactAlarmDialog.collectAsStateWithLifecycle()
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsState(initial = true)

    var showTimePickerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current
    val hapticService: HapticService = koinInject()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationViewModel.setNotificationsEnabled(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.m,
                end = MaterialTheme.spacing.m,
                top = innerPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
        ) {
            item {
                ProfileCard(
                    userData = userData,
                    streakCount = streakCount,
                    onEditProfile = onEditProfile,
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

            // --- NOTIFICATIONS SECTION ---
            item { SectionHeader(icon = Icons.Default.Notifications, title = "Notifications") }
            item {
                SettingsSwitchItem(
                    title = "Enable Notifications",
                    subtitle = "Allow Quill to send you updates",
                    checked = notificationConfig.notificationsEnabled,
                    index = 0,
                    totalCount = 2,
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
                        hapticService.performHaptic(view)
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "Daily Reading Reminder",
                    subtitle = notificationViewModel.formatReminderTime(),
                    checked = notificationConfig.readingRemindersEnabled,
                    enabled = notificationConfig.notificationsEnabled,
                    index = 1,
                    totalCount = 2,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (notificationViewModel.canEnableReminders()) {
                                showTimePickerDialog = true
                            }
                        } else {
                            notificationViewModel.setReadingRemindersEnabled(false)
                            hapticService.performHaptic(view)
                        }
                    },
                    onClick = {
                        if (notificationConfig.notificationsEnabled && notificationViewModel.canEnableReminders()) {
                            showTimePickerDialog = true
                        }
                    }
                )
            }

            // --- AUDIO & HAPTICS SECTION ---
            item { SectionHeader(icon = Icons.AutoMirrored.Filled.VolumeUp, title = "Audio & Haptics") }
            item {
                SettingsSwitchItem(
                    title = "Haptic Feedback",
                    subtitle = "Feel subtle vibrations during interactions",
                    checked = hapticsEnabled,
                    index = 0,
                    totalCount = 1,
                    onCheckedChange = { 
                        settingsViewModel.setHapticsEnabled(it)
                        hapticService.performHaptic(view)
                    }
                )
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

            // --- ABOUT SECTION ---
            item { SectionHeader(icon = Icons.Default.Info, title = "About") }
            item {
                SettingsListItem(
                    title = "About Quill",
                    subtitle = "Version ${com.yugentech.quill.BuildConfig.VERSION_NAME}",
                    index = 0,
                    totalCount = 2,
                    onClick = onAbout,
                )
            }
            item {
                SettingsListItem(
                    title = "What's New",
                    subtitle = "See the latest changes and features",
                    index = 1,
                    totalCount = 2,
                    onClick = onWhatsNew,
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
            initialHour = notificationConfig.reminderTimeHour,
            initialMinute = notificationConfig.reminderTimeMinute,
            onTimeSelected = { hour, minute ->
                notificationViewModel.setReminderTime(hour, minute)
                hapticService.performHaptic(view)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    if (showPermissionDialog) {
        AlarmPermissionDialog(
            context = context,
            onDismiss = { notificationViewModel.dismissPermissionDialog() },
            onConfirm = { notificationViewModel.dismissPermissionDialog() }
        )
    }
}
