package com.yugentech.quill.ui.dash.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.notifications.NotificationsViewModel
import com.yugentech.quill.ui.screens.DiscoverScreen
import com.yugentech.quill.ui.screens.SourcesScreen
import com.yugentech.quill.ui.screens.library.LibraryScreen
import com.yugentech.quill.viewModels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

enum class QuillTab(val title: String, val icon: ImageVector) {
    Library("Library", Icons.Default.LocalLibrary),
    Discover("Discover", Icons.Default.Explore),
    Sources("Sources", Icons.Default.Cloud),
    Settings("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onBookClick: () -> Unit,
    onSignOut: () -> Unit = {},
    onAbout: () -> Unit = {},
    onAppearance: () -> Unit = {},
    settingsViewModel: SettingsViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel()
) {
    var currentTab by remember { mutableStateOf(QuillTab.Library) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 1. Define Screen-Specific Subtitles
    // Using derivedStateOf ensures this only recalculates when currentTab changes
    val dynamicSubtitle by remember {
        derivedStateOf {
            when (currentTab) {
                QuillTab.Library -> "Resume: The Great Gatsby (42%)" // Mock "Last Read"
                QuillTab.Discover -> "Trending: Sci-Fi • Philosophy • Classics"
                QuillTab.Sources -> "Standard Ebooks • Project Gutenberg"
                QuillTab.Settings -> "Customize your sanctuary"
            }
        }
    }

    // 2. Calculate Fade
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    val subtitleAlpha = (1f - collapsedFraction * 1.5f).coerceIn(0f, 1f)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        // 3. The Dynamic Fading Text
                        if (subtitleAlpha > 0f) {
                            Text(
                                text = dynamicSubtitle, // <--- Used here
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.graphicsLayer { alpha = subtitleAlpha }
                            )
                            Spacer(modifier = Modifier.height(4.dp * subtitleAlpha))
                        }

                        // Main Title
                        Text(
                            text = currentTab.title,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                QuillTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                when (tab) {
                    QuillTab.Library -> LibraryScreen(onBookClick = onBookClick)
                    QuillTab.Discover -> DiscoverScreen(onBookClick = onBookClick)
                    QuillTab.Sources -> SourcesScreen(onSourceClick = { })
                    QuillTab.Settings -> SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        notificationsViewModel = notificationsViewModel,
                        onSignOut = onSignOut,
                        onAbout = onAbout,
                        onAppearance = onAppearance
                    )
                }
            }
        }
    }
}