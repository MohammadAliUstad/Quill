package com.yugentech.quill.navigation.navgraph

import android.app.Activity
import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.ui.tabs.libraryScreen.viewmodel.SeeAllViewModel
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.ui.main.parent.MainScreen
import com.yugentech.quill.ui.tabs.libraryScreen.parent.AllBooksScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    context: Context
) {
    composable(AppScreen.Main.route) {
        val libraryViewModel: LibraryViewModel = koinViewModel()
        val authViewModel: AuthViewModel = koinViewModel()
        MainScreen(
            libraryViewModel = libraryViewModel,
            onLibraryBookClick = { book ->
                navController.navigate(AppScreen.BookDetailsScreen.createRoute(book = book)) {
                    launchSingleTop = true
                }
            },
            onDiscoverBookClick = { book ->
                navController.navigate(AppScreen.BookDetailsScreen.createRoute(book = book)) {
                    launchSingleTop = true
                }
            },
            onSeeAllClick = { categoryName ->
                navController.navigate(AppScreen.AllBooks.route + "/$categoryName") {
                    launchSingleTop = true
                }
            },
            onResumeClick = { book ->
                context.startActivity(ReaderActivity.createIntent(context, book.id, null))
            },
            onSourceClick = { sourceId ->
                when (sourceId) {
                    BookSource.STANDARD_EBOOKS -> navController.navigate(AppScreen.StandardEbooks.route) {
                        launchSingleTop = true
                    }

                    BookSource.GUTENBERG -> navController.navigate(AppScreen.Gutenberg.route) {
                        launchSingleTop = true
                    }

                    BookSource.USER_IMPORTED -> {}
                }
            },
            onAboutClick = {
                navController.navigate(AppScreen.About.route) { launchSingleTop = true }
            },
            onAppearanceClick = {
                navController.navigate(AppScreen.Appearance.route) { launchSingleTop = true }
            },
            onManageStorage = {
                navController.navigate(AppScreen.Storage.route) { launchSingleTop = true }
            },
            onManageCategories = {
                navController.navigate(AppScreen.ManageCategories.route) { launchSingleTop = true }
            },
            onAiraSettings = {
                navController.navigate(AppScreen.AboutAira.route) { launchSingleTop = true }
            },
            onEditProfile = {
                navController.navigate(AppScreen.EditProfileScreen.route) { launchSingleTop = true }
            },
            onViewInsights = {
                navController.navigate(AppScreen.Insights.route) { launchSingleTop = true }
            },
            onSubscriptions = {
                navController.navigate(AppScreen.Subscriptions.route) { launchSingleTop = true }
            },
            onExitApp = {
                (context as? Activity)?.finishAffinity()
            },
            onSignOut = {
                authViewModel.signOut()
            },
            onViewIndexingQueue = {
                navController.navigate(AppScreen.Queue.route) { launchSingleTop = true }
            },
            onWhatsNew = {
                navController.navigate(AppScreen.WhatsNew.route) { launchSingleTop = true }
            },
        )
    }

    composable(
        route = AppScreen.AllBooks.route + "/{categoryName}",
        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
    ) { backStackEntry ->
        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
        val seeAllViewModel: SeeAllViewModel = koinViewModel(
            parameters = { parametersOf(categoryName) }
        )

        AllBooksScreen(
            viewModel = seeAllViewModel,
            onBackClick = { navController.popBackStack() },
            onBookClick = { book ->
                navController.navigate(AppScreen.BookDetailsScreen.createRoute(book = book)) {
                    launchSingleTop = true
                }
            }
        )
    }
}