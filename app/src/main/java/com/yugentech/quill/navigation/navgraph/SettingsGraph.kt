package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.storage.viewmodel.StorageViewModel
import com.yugentech.quill.ui.info.storage.parent.StorageScreen
import com.yugentech.quill.ui.info.indexing.parent.IndexingQueueScreen
import com.yugentech.quill.ui.info.indexing.viewmodel.IndexingViewModel
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController
) {
    composable(AppScreen.Queue.route) {
        val viewModel: IndexingViewModel = koinViewModel()
        IndexingQueueScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.Appearance.route) {
        val themeViewModel: ThemeViewModel = koinViewModel()
        com.yugentech.quill.ui.config.appearance.parent.AppearanceScreen(
            themeViewModel = themeViewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.ManageCategories.route) {
        val categoryViewModel: CategoryViewModel = koinViewModel()
        com.yugentech.quill.ui.config.category.parent.CategoryScreen(
            categoryViewModel = categoryViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.Storage.route) {
        val storageViewModel: StorageViewModel = koinViewModel()
        StorageScreen(
            storageViewModel = storageViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }
}