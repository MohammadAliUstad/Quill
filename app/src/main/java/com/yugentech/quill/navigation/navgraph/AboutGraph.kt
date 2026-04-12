package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.about.aira.parent.AiraAboutScreen
import com.yugentech.quill.ui.about.about.parent.AboutScreen
import com.yugentech.quill.ui.about.about.parent.AboutViewModel
import com.yugentech.quill.ui.about.attributions.parent.AttributionsScreen
import com.yugentech.quill.ui.about.about.parent.MoreAppsScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.aboutGraph(
    navController: NavHostController
) {
    composable(AppScreen.AboutAira.route) {
        AiraAboutScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.About.route) {

        val aboutViewModel: AboutViewModel = koinViewModel()

        AboutScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLicenses = {
                navController.navigate(AppScreen.Licenses.route) {
                    launchSingleTop = true
                }
            },
            onNavigateToMoreApps = {
                navController.navigate(AppScreen.MoreApps.route) {
                    launchSingleTop = true
                }
            },
            viewModel = aboutViewModel
        )
    }

    composable(AppScreen.Licenses.route) {
        AttributionsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.MoreApps.route) {
        MoreAppsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}