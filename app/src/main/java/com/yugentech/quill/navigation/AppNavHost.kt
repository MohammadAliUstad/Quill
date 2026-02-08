@file:Suppress("DEPRECATION")

package com.yugentech.quill.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.yugentech.quill.bookDetails.BookDetailsViewModel
import com.yugentech.quill.library.LibraryViewModel
import com.yugentech.quill.theme.ThemeViewModel
import com.yugentech.quill.ui.config.screens.AboutScreen
import com.yugentech.quill.ui.config.screens.AppearanceScreen
import com.yugentech.quill.ui.config.screens.AttributionsScreen
import com.yugentech.quill.ui.dash.screens.airaScreen.AiraChatScreen
import com.yugentech.quill.ui.dash.screens.mainScreen.parent.MainScreen
import com.yugentech.quill.ui.dash.utils.defaultEnterTransition
import com.yugentech.quill.ui.dash.utils.defaultExitTransition
import com.yugentech.quill.ui.dash.utils.defaultPopEnterTransition
import com.yugentech.quill.ui.dash.utils.defaultPopExitTransition
import com.yugentech.quill.ui.dash.screens.readerScreen.parent.ReaderScreen
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.parent.BookDetailsScreen
import com.yugentech.quill.ui.dash.screens.ManageCategoriesScreen
import com.yugentech.quill.ui.dash.screens.standardScreen.parent.StandardScreen
import com.yugentech.quill.viewModels.StandardViewModel
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = Screens.Main.route,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        composable(Screens.Licenses.route) {
            AttributionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screens.Main.route) {
            val libraryViewModel: LibraryViewModel = koinViewModel()

            MainScreen(
                onLibraryBookClick = { book ->
                    val bookJson = Json.encodeToString(book)
                    val encodedJson = URLEncoder.encode(bookJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("${Screens.BookDetailsScreen.route}/$encodedJson")
                },
                onDiscoverItemClick = { },
                onSourceClick = { sourceId ->
                    if (sourceId == "standard_ebooks") {
                        navController.navigate(Screens.StandardEbooks.route)
                    }
                },
                onAboutClick = {
                    navController.navigate(Screens.About.route)
                },
                onAppearanceClick = {
                    navController.navigate(Screens.Appearance.route)
                },
                onManageCategories = {
                    navController.navigate(Screens.ManageCategories.route)
                },
                onFABClick = {
                    navController.navigate(Screens.Aira.route)
                },
                libraryViewModel = libraryViewModel
            )
        }

        composable(Screens.Aira.route) {
            AiraChatScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screens.ManageCategories.route) {
            ManageCategoriesScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screens.StandardEbooks.route) {
            val standardViewModel: StandardViewModel = koinViewModel()
            StandardScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { book ->
                    val bookJson = Json.encodeToString(book)
                    val encodedJson = URLEncoder.encode(bookJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("${Screens.BookDetailsScreen.route}/$encodedJson")
                },
                standardViewModel = standardViewModel
            )
        }

        // ... inside AppNavHost content

        composable(
            route = "${Screens.BookDetailsScreen.route}/{book_json}",
            arguments = listOf(
                navArgument("book_json") { type = NavType.StringType }
            )
        ) {
            val bookDetailsViewModel: BookDetailsViewModel = koinViewModel()

            BookDetailsScreen(
                onBackClick = { navController.popBackStack() },
                // Inside AppNavHost composable for BookDetailsScreen
                onReadClick = { bookId, chapterHref ->
                    // FIX: Always encode the bookId before passing it in a route
                    val encodedBookId = URLEncoder.encode(bookId, StandardCharsets.UTF_8.toString())

                    if (chapterHref != null) {
                        val encodedHref = URLEncoder.encode(chapterHref, StandardCharsets.UTF_8.toString())
                        navController.navigate("reader/$encodedBookId?href=$encodedHref")
                    } else {
                        navController.navigate("reader/$encodedBookId")
                    }
                },
                onManageCategoriesClick = {
                    navController.navigate(Screens.ManageCategories.route)
                },
                bookDetailsViewModel = bookDetailsViewModel
            )
        }

        composable(
            route = "reader/{bookId}?href={href}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("href") {
                    type = NavType.StringType
                    nullable = true // It's optional!
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
            val href = backStackEntry.arguments?.getString("href")

            ReaderScreen(
                bookId = bookId,
                initialChapterHref = href,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Defines the appearance settings screen
        composable(Screens.Appearance.route) {
            val themeViewModel: ThemeViewModel = koinViewModel()
            AppearanceScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                themeViewModel = themeViewModel
            )
        }

        // Defines the about screen
        composable(Screens.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLicenses = {
                    navController.navigate(Screens.Licenses.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}