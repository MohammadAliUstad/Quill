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
import androidx.compose.material.icons.filled.Backup
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
import androidx.compose.runtime.LaunchedEffect
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
import com.yugentech.quill.auth.guest.GuestBackupViewModel
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
import com.yugentech.quill.ui.tabs.moreScreen.components.dialogs.BackupResultDialog
import com.yugentech.quill.ui.tabs.moreScreen.components.dialogs.ExitGuestDialog
import com.yugentech.quill.ui.tabs.moreScreen.components.dialogs.TimerPickerDialog
import com.yugentech.quill.notification.viewmodel.NotificationViewModel
import com.yugentech.quill.ui.tabs.moreScreen.viewmodel.SettingsViewModel
import com.yugentech.theme.tokens.spacing
import org.koin.androidx.compose.koinViewModel
import com.yugentech.quill.localization.AppLocale
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onLanguage: () -> Unit,
    isGuest: Boolean = false,
    onExitGuest: () -> Unit = {},
    notificationViewModel: NotificationViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    guestBackupViewModel: GuestBackupViewModel = koinViewModel()
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

    // A guest's backup file: saved where they choose, and read back from any file they pick
    // (file managers don't all call .json files JSON, hence the wider list)
    val backupState by guestBackupViewModel.state.collectAsStateWithLifecycle()
    // Set by "Export and exit" in the exit-guest dialog: once the file is saved, guest mode ends.
    // Cancelling the save or a failed export keeps them in guest mode. Plain remember, not
    // rememberSaveable: the tabs restore saved state, so leaving More mid-export would bring
    // this back later and sign them out by surprise. Dropping it there just shows "Backup saved".
    var exitAfterExport by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) guestBackupViewModel.export(uri) else exitAfterExport = false
    }
    LaunchedEffect(backupState.result, exitAfterExport) {
        if (!exitAfterExport) return@LaunchedEffect
        when (backupState.result) {
            GuestBackupViewModel.Result.Exported -> {
                exitAfterExport = false
                guestBackupViewModel.dismissResult()
                onExitGuest()
            }
            GuestBackupViewModel.Result.ExportFailed -> exitAfterExport = false
            else -> {}
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(guestBackupViewModel::import) }
    fun launchExport() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        exportLauncher.launch("quill-backup-$date.json")
    }
    fun launchImport() {
        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
    }
    var showExitGuestDialog by remember { mutableStateOf(false) }

    // A guest has no name until they pick one, so they're shown as "Guest"
    val guestName = stringResource(R.string.guest_name)
    val shownUser = if (isGuest && userData.name.isNullOrBlank()) userData.copy(name = guestName) else userData

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
                            text = stringResource(R.string.more_title),
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
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
        ) {
            item {
                ProfileCard(
                    userData = shownUser,
                    streakCount = streakCount,
                    onEditName = { showNameSheet = true },
                    onEditAvatar = { showAvatarSheet = true },
                    onViewInsights = onViewInsights
                )
            }

            // --- AI SECTION ---
            // Aira needs an account, so a guest has one row: Meet Aira, which opens her introduction
            // in place of the subscription screen and says what an account brings
            val aiItemCount = when {
                isGuest -> 1
                isIndexingActive -> 3
                else -> 2
            }
            item { SectionHeader(icon = Icons.Default.AutoAwesome, title = stringResource(R.string.more_section_ai)) }
            if (!isGuest) {
                item {
                    SettingsListItem(
                        title = stringResource(R.string.more_meet_aira),
                        subtitle = stringResource(R.string.more_meet_aira_subtitle),
                        index = 0,
                        totalCount = aiItemCount,
                        onClick = onAboutAira,
                    )
                }
            }
            item {
                SettingsListItem(
                    title = stringResource(if (isGuest) R.string.more_meet_aira else R.string.more_subscriptions),
                    subtitle = stringResource(if (isGuest) R.string.more_meet_aira_subtitle else R.string.more_subscriptions_subtitle),
                    index = if (isGuest) 0 else 1,
                    totalCount = aiItemCount,
                    onClick = onSubscriptions,
                )
            }
            if (isIndexingActive && !isGuest) {
                item(key = "indexing_queue") {
                    SettingsListItem(
                        modifier = Modifier.animateItem(),
                        title = stringResource(R.string.more_indexing),
                        subtitle = stringResource(R.string.more_indexing_subtitle),
                        index = 2,
                        totalCount = aiItemCount,
                        onClick = onViewIndexingQueue,
                    )
                }
            }

            // --- LIBRARY SECTION ---
            item { SectionHeader(icon = Icons.AutoMirrored.Filled.List, title = stringResource(R.string.more_section_library)) }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_manage_categories),
                    subtitle = stringResource(R.string.more_manage_categories_subtitle),
                    index = 0,
                    totalCount = 2,
                    onClick = onManageCategories,
                )
            }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_manage_storage),
                    subtitle = stringResource(R.string.more_manage_storage_subtitle),
                    index = 1,
                    totalCount = 2,
                    onClick = onManageStorage,
                )
            }

            // --- APPEARANCE SECTION ---
            item { SectionHeader(icon = Icons.Default.Palette, title = stringResource(R.string.more_section_appearance)) }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_theme_colors),
                    subtitle = stringResource(R.string.more_theme_colors_subtitle),
                    index = 0,
                    totalCount = 2,
                    onClick = onAppearance,
                )
            }
            item {
                val context = LocalContext.current
                SettingsListItem(
                    title = stringResource(R.string.language_title),
                    subtitle = AppLocale.current(context)?.nativeName
                        ?: stringResource(R.string.language_system_default),
                    index = 1,
                    totalCount = 2,
                    onClick = onLanguage,
                )
            }

            // --- NOTIFICATIONS & HAPTICS SECTION ---
            item { SectionHeader(icon = Icons.Default.Notifications, title = stringResource(R.string.more_section_notifications)) }
            item {
                SettingsSwitchItem(
                    title = stringResource(R.string.more_enable_notifications),
                    subtitle = stringResource(R.string.more_enable_notifications_subtitle),
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
                    title = stringResource(R.string.more_daily_reminder),
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
                    title = stringResource(R.string.more_playful_reminders),
                    subtitle = stringResource(R.string.more_playful_reminders_subtitle),
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
                    title = stringResource(R.string.more_haptics),
                    subtitle = stringResource(R.string.more_haptics_subtitle),
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
            item { SectionHeader(icon = Icons.Default.Info, title = stringResource(R.string.more_section_about)) }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_about_quill),
                    subtitle = stringResource(R.string.more_version, com.yugentech.quill.BuildConfig.VERSION_NAME),
                    index = 0,
                    totalCount = 4,
                    onClick = onAbout,
                )
            }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_whats_new),
                    subtitle = stringResource(R.string.more_whats_new_subtitle),
                    index = 1,
                    totalCount = 4,
                    onClick = onWhatsNew,
                )
            }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_replay_onboarding),
                    subtitle = stringResource(R.string.more_replay_onboarding_subtitle),
                    index = 2,
                    totalCount = 4,
                    onClick = onReplayOnboarding,
                )
            }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_more_from_us),
                    subtitle = stringResource(R.string.more_more_from_us_subtitle),
                    index = 3,
                    totalCount = 4,
                    onClick = { showMoreApps = true },
                )
            }

            // --- BACKUP SECTION ---
            // A library saved to a file and brought back. A guest's lives only on this phone, so
            // it's their way to keep it; a signed-in user's file also holds what the cloud copy
            // doesn't (highlights, reading time), and what they import syncs to their account
            item {
                SectionHeader(
                    icon = Icons.Default.Backup,
                    title = stringResource(R.string.guest_backup_section)
                )
            }
            item {
                SettingsListItem(
                    title = stringResource(R.string.guest_export_title),
                    subtitle = stringResource(if (isGuest) R.string.guest_export_subtitle else R.string.account_export_subtitle),
                    index = 0,
                    totalCount = 2,
                    onClick = { if (!backupState.busy) launchExport() },
                )
            }
            item {
                SettingsListItem(
                    title = stringResource(R.string.guest_import_title),
                    subtitle = stringResource(if (isGuest) R.string.guest_import_subtitle else R.string.account_import_subtitle),
                    index = 1,
                    totalCount = 2,
                    onClick = { if (!backupState.busy) launchImport() },
                )
            }

            // --- SESSION SECTION ---
            item { SectionHeader(icon = Icons.AutoMirrored.Filled.ExitToApp, title = stringResource(R.string.more_section_session)) }
            item {
                SettingsListItem(
                    title = stringResource(R.string.more_exit),
                    subtitle = stringResource(R.string.more_exit_subtitle),
                    index = 0,
                    totalCount = 2,
                    onClick = onExit,
                )
            }
            // For a guest, signing out becomes leaving guest mode, which says what it costs
            item {
                SettingsListItem(
                    title = stringResource(if (isGuest) R.string.guest_exit_title else R.string.more_sign_out),
                    subtitle = stringResource(if (isGuest) R.string.guest_exit_subtitle else R.string.more_sign_out_subtitle),
                    index = 1,
                    totalCount = 2,
                    onClick = { if (isGuest) showExitGuestDialog = true else onSignOut() },
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

    if (showExitGuestDialog) {
        ExitGuestDialog(
            onConfirm = {
                showExitGuestDialog = false
                onExitGuest()
            },
            onExportAndExit = {
                showExitGuestDialog = false
                if (!backupState.busy) {
                    exitAfterExport = true
                    launchExport()
                }
            },
            onDismiss = { showExitGuestDialog = false }
        )
    }

    // After "Export and exit" a successful save leads straight out, so no "Backup saved" first
    backupState.result?.takeUnless { exitAfterExport && it == GuestBackupViewModel.Result.Exported }?.let { result ->
        val dismiss = { guestBackupViewModel.dismissResult() }
        when (result) {
            GuestBackupViewModel.Result.Exported -> BackupResultDialog(
                success = true,
                title = stringResource(R.string.guest_export_done_title),
                message = stringResource(R.string.guest_export_done_message),
                onDismiss = dismiss
            )
            GuestBackupViewModel.Result.ExportFailed -> BackupResultDialog(
                success = false,
                title = stringResource(R.string.guest_export_failed_title),
                message = stringResource(R.string.guest_export_failed_message),
                onDismiss = dismiss
            )
            is GuestBackupViewModel.Result.Imported -> BackupResultDialog(
                success = true,
                title = stringResource(R.string.guest_import_done_title),
                message = stringResource(R.string.guest_import_done_message, result.books, result.highlights),
                onDismiss = dismiss
            )
            GuestBackupViewModel.Result.ImportFailed -> BackupResultDialog(
                success = false,
                title = stringResource(R.string.guest_import_failed_title),
                message = stringResource(R.string.guest_import_failed_message),
                onDismiss = dismiss
            )
        }
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
