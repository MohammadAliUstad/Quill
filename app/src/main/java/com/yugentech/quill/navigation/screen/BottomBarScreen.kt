package com.yugentech.quill.navigation.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.ui.graphics.vector.ImageVector

// Defines the main bottom navigation screens with icons
sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Library : BottomBarScreen(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    )

    data object Discover : BottomBarScreen(
        route = "discover",
        title = "Discover",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Sources : BottomBarScreen(
        route = "sources",
        title = "Sources",
        selectedIcon = Icons.Filled.Source,
        unselectedIcon = Icons.Outlined.Source
    )

    data object Settings : BottomBarScreen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    companion object {
        // Maps a string route to its corresponding screen object
        fun fromRoute(route: String): BottomBarScreen = when (route) {
            Library.route -> Library
            Discover.route -> Discover
            Sources.route -> Sources
            Settings.route -> Settings
            else -> Library
        }
    }
}