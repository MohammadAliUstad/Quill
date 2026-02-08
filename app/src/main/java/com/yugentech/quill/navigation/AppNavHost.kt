@file:Suppress("DEPRECATION")

package com.yugentech.quill.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.yugentech.quill.network.remote.Book
import com.yugentech.quill.theme.ThemeViewModel
import com.yugentech.quill.ui.config.screens.AboutScreen
import com.yugentech.quill.ui.config.screens.AppearanceScreen
import com.yugentech.quill.ui.config.screens.AttributionsScreen
import com.yugentech.quill.ui.dash.screens.AiraChatScreen
import com.yugentech.quill.ui.dash.screens.mainScreen.MainScreen
import com.yugentech.quill.ui.dash.utils.defaultEnterTransition
import com.yugentech.quill.ui.dash.utils.defaultExitTransition
import com.yugentech.quill.ui.dash.utils.defaultPopEnterTransition
import com.yugentech.quill.ui.dash.utils.defaultPopExitTransition
import com.yugentech.quill.ui.dash.screens.readerScreen.parent.ReaderScreen
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.parent.BookDetailsScreen
import com.yugentech.quill.ui.dash.screens.ManageCategoriesScreen
import com.yugentech.quill.ui.dash.screens.StandardScreen
import com.yugentech.quill.viewModels.StandardViewModel
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel
import java.net.URLDecoder
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
            MainScreen(
                onLibraryBookClick = { book ->
                    val bookJson = Json.encodeToString(book)
                    val encodedJson = URLEncoder.encode(bookJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("${Screens.BookDetailsScreen.route}/$encodedJson")
                },
                onDiscoverItemClick = { book ->
                    val bookJson = Json.encodeToString(book)
                    val encodedJson =
                        URLEncoder.encode(bookJson, StandardCharsets.UTF_8.toString())
                    navController.navigate("${Screens.BookDetailsScreen.route}/$encodedJson")
                },
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
                }
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
            route = "${Screens.BookDetailsScreen.route}/{bookJson}",
            arguments = listOf(navArgument("bookJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookJson = backStackEntry.arguments?.getString("bookJson")
            if (bookJson != null) {
                // Decode JSON back to Book object
                val decodedJson = URLDecoder.decode(bookJson, StandardCharsets.UTF_8.toString())
                val book = Json.decodeFromString<Book>(decodedJson)

                BookDetailsScreen(
                    book = book,
                    onBackClick = { navController.popBackStack() },
                    onReadClick = { bookId ->
                        // Navigate to the ReaderScreen using the ID passed from BookDetails
                        navController.navigate(Screens.Reader.createRoute(bookId))
                    },
                    onManageCategoriesClick = {
                        // Navigate to Manage Categories if needed
                        navController.navigate(Screens.ManageCategories.route)
                    }
                )
            }
        }

        // In AppNavHost.kt

        composable(
            route = "reader/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable

            ReaderScreen(
                bookId = bookId,
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