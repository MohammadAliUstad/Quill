package com.yugentech.quill.navigation.navgraph

import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.theme.ThemeViewModel
import com.yugentech.quill.ui.more.aboutScreen.parent.AboutScreen
import com.yugentech.quill.ui.more.appearanceScreen.components.AppearanceScreen
import com.yugentech.quill.ui.more.attributions.parent.AttributionsScreen
import com.yugentech.quill.ui.more.categoryScreen.parent.CategoryScreen
import com.yugentech.quill.ui.more.storageScreen.parent.StorageScreen
import com.yugentech.quill.ui.shared.airaScreen.parent.AiraChatScreen
import com.yugentech.quill.ui.shared.bookDetailsScreen.parent.BookDetailsScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.detailGraph(
    navController: NavHostController,
    context: Context
) {
    // Defines the book details screen
    composable(
        route = AppScreen.BookDetailsScreen.routeWithArgs,
        arguments = AppScreen.BookDetailsScreen.arguments
    ) {
        val bookDetailsViewModel: BookDetailsViewModel = koinViewModel()
        BookDetailsScreen(
            bookDetailsViewModel = bookDetailsViewModel,
            onBackClick = { navController.popBackStack() },
            onReadClick = { bookId, chapterHref ->
                context.startActivity(ReaderActivity.createIntent(context, bookId, chapterHref))
            },
            onAiraClick = { bookId ->
                navController.navigate(AppScreen.Aira.route + "/$bookId") {
                    launchSingleTop = true
                }
            }
        )
    }

    // Defines the Aira chat screen
    composable(
        route = AppScreen.Aira.route + "/{bookId}",
        arguments = listOf(
            navArgument("bookId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
        val airaViewModel: AiraViewModel = koinViewModel()
        AiraChatScreen(
            onBackClick = { navController.popBackStack() },
            bookId = bookId,
            viewModel = airaViewModel
        )
    }

    // Defines the appearance settings screen
    composable(AppScreen.Appearance.route) {
        val themeViewModel: ThemeViewModel = koinViewModel()
        AppearanceScreen(
            themeViewModel = themeViewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    // Defines the manage categories screen
    composable(AppScreen.ManageCategories.route) {
        val categoryViewModel: CategoryViewModel = koinViewModel()
        CategoryScreen(
            categoryViewModel = categoryViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    // Defines the storage management screen
    composable(AppScreen.Storage.route) {
        StorageScreen(
            onBackClick = { navController.popBackStack() }
        )
    }

    // Defines the about screen
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

    // Defines the licenses/attributions screen
    composable(AppScreen.Licenses.route) {
        AttributionsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}