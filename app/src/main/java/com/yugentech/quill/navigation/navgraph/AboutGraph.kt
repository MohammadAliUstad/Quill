package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.more.aboutAira.AiraAboutScreen
import com.yugentech.quill.ui.more.aboutScreen.parent.AboutScreen
import com.yugentech.quill.ui.more.attributionsScreen.parent.AttributionsScreen
import com.yugentech.quill.ui.more.contributorsScreen.ContributorsScreen

fun NavGraphBuilder.aboutGraph(
    navController: NavHostController
) {
    composable(AppScreen.AboutAira.route) {
        AiraAboutScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.Contributors.route) {
        ContributorsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.About.route) {
        AboutScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLicenses = {
                navController.navigate(AppScreen.Licenses.route) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppScreen.Licenses.route) {
        AttributionsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}