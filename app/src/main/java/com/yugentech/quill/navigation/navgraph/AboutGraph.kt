package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.about.aboutAira.AiraAboutScreen
import com.yugentech.quill.ui.about.aboutScreen.parent.AboutScreen
import com.yugentech.quill.ui.about.attributionsScreen.parent.AttributionsScreen
import com.yugentech.quill.ui.about.contributorsScreen.ContributorsScreen

fun NavGraphBuilder.aboutGraph(
    navController: NavHostController
) {
    composable(AppScreen.AboutAira.route) {
        _root_ide_package_.com.yugentech.quill.ui.about.aboutAira.AiraAboutScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.Contributors.route) {
        _root_ide_package_.com.yugentech.quill.ui.about.contributorsScreen.ContributorsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.About.route) {
        _root_ide_package_.com.yugentech.quill.ui.about.aboutScreen.parent.AboutScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLicenses = {
                navController.navigate(AppScreen.Licenses.route) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppScreen.Licenses.route) {
        _root_ide_package_.com.yugentech.quill.ui.about.attributionsScreen.parent.AttributionsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}