package com.yugentech.quill.ui.tabs.moreScreen.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.R
import com.yugentech.theme.tokens.components
import com.yugentech.theme.tokens.spacing
import com.yugentech.quill.ui.mainScreen.components.SectionHeader
import com.yugentech.quill.ui.tabs.moreScreen.components.SettingsListItem
import com.yugentech.quill.ui.tabs.moreScreen.components.SettingsSwitchItem

@Composable
fun SettingsScreen(
    onAbout: () -> Unit,
    onAppearance: () -> Unit,
    onManageCategories: () -> Unit,
    onManageStorage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.m,
            end = MaterialTheme.spacing.m,
            bottom = MaterialTheme.components.bottomNavHeight
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)
    ) {
        // --- NEW SECTION: LIBRARY ---
        item {
            SectionHeader(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Library"
            )
        }
        item {
            SettingsListItem(
                title = "Manage Categories",
                subtitle = "Add, remove, or reorder your bookshelves",
                index = 0,
                totalCount = 2, // UPDATED: Now 2 items in this section
                onClick = onManageCategories
            )
        }
        // --- NEW STORAGE OPTION ---
        item {
            SettingsListItem(
                title = "Manage Storage",
                subtitle = "View device storage and remove downloads",
                index = 1,
                totalCount = 2,
                onClick = onManageStorage
            )
        }

        // --- EXISTING SECTIONS ---

        item {
            SectionHeader(
                icon = Icons.Default.Notifications,
                title = "Notifications"
            )
        }
        item {
            SettingsSwitchItem(
                title = "Enable Notifications",
                subtitle = "Allow Sessions to send you notifications",
                checked = true,
                index = 0,
                totalCount = 2,
                onCheckedChange = { }
            )
        }

        item {
            SectionHeader(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Audio & Haptics"
            )
        }
        item {
            SettingsSwitchItem(
                title = "Sound Effects",
                subtitle = "Play sounds for timer events",
                checked = true,
                index = 0,
                totalCount = 2,
                onCheckedChange = { }
            )
        }
        item {
            SettingsSwitchItem(
                title = "Haptic Feedback",
                subtitle = "Feel vibrations for timer events",
                checked = true,
                index = 1,
                totalCount = 2,
                onCheckedChange = { }
            )
        }

        item {
            SectionHeader(
                icon = Icons.Default.Palette,
                title = "Appearance"
            )
        }
        item {
            SettingsListItem(
                title = "Theme & Colors",
                subtitle = "Customize your app's look and feel",
                index = 0,
                totalCount = 1,
                onClick = onAppearance
            )
        }

        item {
            SectionHeader(
                icon = Icons.Default.Info,
                title = "About"
            )
        }
        item {
            SettingsListItem(
                title = "About Sessions",
                subtitle = stringResource(R.string.version),
                index = 0,
                totalCount = 2,
                onClick = onAbout
            )
        }
        item {
            SettingsListItem(
                title = "Sign Out",
                subtitle = "Log out of your current session",
                index = 1,
                totalCount = 2,
                onClick = { }
            )
        }
    }
}