package com.yugentech.quill.navigation.navgraph

import android.app.Activity
import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.ui.tabs.libraryScreen.allBooks.viewmodel.SeeAllViewModel
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.domain.AuthRepository
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.ui.main.parent.MainScreen
import com.yugentech.quill.ui.tabs.libraryScreen.allBooks.parent.AllBooksScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.compose.runtime.remember
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    context: Context
) {
    composable(AppScreen.Main.route) {
        val libraryViewModel: LibraryViewModel = koinViewModel()
        val authViewModel: AuthViewModel = koinViewModel()
        val authRepository: AuthRepository = koinInject()
        // Fixed while this screen is up: entering or leaving guest mode always goes through sign-in
        val userId = remember { authRepository.currentUser.orEmpty() }
        val isGuest = remember { authRepository.isGuest }
        MainScreen(
            userId = userId,
            isGuest = isGuest,
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
            onShelfClick = { shelf ->
                navController.navigate(AppScreen.Shelf.createRoute(shelf)) { launchSingleTop = true }
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
                // A guest has no account to come back to, so leaving removes their library
                if (isGuest) authViewModel.exitGuestMode() else authViewModel.signOut()
            },
            onViewIndexingQueue = {
                navController.navigate(AppScreen.Queue.route) { launchSingleTop = true }
            },
            onWhatsNew = {
                navController.navigate(AppScreen.WhatsNew.route) { launchSingleTop = true }
            },
            onMoreApps = {
                navController.navigate(AppScreen.MoreApps.route) { launchSingleTop = true }
            },
            onReplayOnboarding = {
                navController.navigate(AppScreen.OnboardingReplay.route) { launchSingleTop = true }
            },
            onLanguage = {
                navController.navigate(AppScreen.Language.route) { launchSingleTop = true }
            },
            onNavigateRoute = { route ->
                navController.navigate(route) { launchSingleTop = true }
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