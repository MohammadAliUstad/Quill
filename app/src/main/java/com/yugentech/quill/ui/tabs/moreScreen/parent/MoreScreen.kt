package com.yugentech.quill.ui.tabs.moreScreen.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.UserData
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.quill.ui.tabs.moreScreen.components.ProfileCard
import com.yugentech.quill.ui.tabs.moreScreen.components.SettingsListItem
import com.yugentech.theme.tokens.spacing

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
) {
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
                bottom = contentPadding.calculateBottomPadding() + 8.dp
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

            val aiItemCount = if (isIndexingActive) 3 else 2

            item {
                SectionHeader(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI",
                )
            }
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

            item {
                SectionHeader(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "Library",
                )
            }
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

            item {
                SectionHeader(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                )
            }
            item {
                SettingsListItem(
                    title = "Theme & Colors",
                    subtitle = "Customize your app's look and feel",
                    index = 0,
                    totalCount = 1,
                    onClick = onAppearance,
                )
            }

            item {
                SectionHeader(
                    icon = Icons.Default.Info,
                    title = "About",
                )
            }
            item {
                SettingsListItem(
                    title = "About Quill",
                    subtitle = "Version 3.1.0",
                    index = 0,
                    totalCount = 1,
                    onClick = onAbout,
                )
            }

            item {
                SectionHeader(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Session",
                )
            }
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
}