package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.storage.StorageViewModel
import com.yugentech.quill.ui.more.appearanceScreen.parent.AppearanceScreen
import com.yugentech.quill.ui.more.categoryScreen.parent.CategoryScreen
import com.yugentech.quill.ui.more.storageScreen.parent.StorageScreen
import com.yugentech.quill.ui.more.indexingQueueScreen.IndexingQueueScreen
import com.yugentech.quill.viewmodel.indexing.IndexingQueueViewModel
import com.yugentech.quill.theme.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController
) {
    composable(AppScreen.Queue.route) {
        val viewModel: IndexingQueueViewModel = koinViewModel()
        IndexingQueueScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.Appearance.route) {
        val themeViewModel: ThemeViewModel = koinViewModel()
        AppearanceScreen(
            themeViewModel = themeViewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.ManageCategories.route) {
        val categoryViewModel: CategoryViewModel = koinViewModel()
        CategoryScreen(
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