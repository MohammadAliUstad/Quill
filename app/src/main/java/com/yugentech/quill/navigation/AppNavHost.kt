@file:Suppress("DEPRECATION")

package com.yugentech.quill.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yugentech.quill.bookDetails.BookDetailsViewModel
import com.yugentech.quill.category.CategoryViewModel
import com.yugentech.quill.library.AllBooksArgs
import com.yugentech.quill.library.LibraryViewModel
import com.yugentech.quill.network.api.GutenbergViewModel
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.standardEBooks.StandardViewModel
import com.yugentech.quill.theme.ThemeViewModel
import com.yugentech.quill.ui.config.screens.AboutScreen
import com.yugentech.quill.ui.config.screens.AppearanceScreen
import com.yugentech.quill.ui.config.screens.AttributionsScreen
import com.yugentech.quill.ui.dash.screens.StorageScreen
import com.yugentech.quill.ui.dash.screens.airaScreen.AiraChatScreen
import com.yugentech.quill.ui.dash.screens.bookDetailsScreen.parent.BookDetailsScreen
import com.yugentech.quill.ui.dash.screens.categoryScreen.parent.CategoryScreen
import com.yugentech.quill.ui.dash.screens.standardScreen.parent.GutenbergScreen
import com.yugentech.quill.ui.dash.screens.libraryScreen.parent.AllBooksScreen
import com.yugentech.quill.ui.dash.screens.mainScreen.parent.MainScreen
import com.yugentech.quill.ui.dash.screens.standardScreen.parent.StandardScreen
import com.yugentech.quill.ui.dash.utils.defaultEnterTransition
import com.yugentech.quill.ui.dash.utils.defaultExitTransition
import com.yugentech.quill.ui.dash.utils.defaultPopEnterTransition
import com.yugentech.quill.ui.dash.utils.defaultPopExitTransition
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController
) {
    // Context is needed to start the Activity directly
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screens.Main.route,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        composable(Screens.Main.route) {
            val libraryViewModel: LibraryViewModel = koinViewModel()

            MainScreen(
                onLibraryBookClick = { book ->
                    navController.navigate(Screens.BookDetailsScreen.createRoute(bookId = book.id))
                },
                onSeeAllClick = { title, books ->
                    AllBooksArgs.title = title
                    AllBooksArgs.books = books
                    navController.navigate("all_books")
                },
                onResumeClick = { book ->
                    context.startActivity(
                        ReaderActivity.createIntent(context, book.id, null)
                    )
                },
                onSourceClick = { sourceId ->
                    if (sourceId == "standard_ebooks") {
                        navController.navigate(Screens.StandardEbooks.route)
                    } else if (sourceId == "gutenberg") {
                        navController.navigate(Screens.Gutenberg.route)
                    }
                },
                onAboutClick = { navController.navigate(Screens.About.route) },
                onAppearanceClick = { navController.navigate(Screens.Appearance.route) },
                onManageStorage = { navController.navigate(Screens.Storage.route) },
                onManageCategories = { navController.navigate(Screens.ManageCategories.route) },
                libraryViewModel = libraryViewModel
            )
        }

        composable(Screens.Gutenberg.route) {
            val gutenbergViewModel: GutenbergViewModel = koinViewModel()
            GutenbergScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateByContent = { book ->
                    navController.navigate(Screens.BookDetailsScreen.createRoute(book = book))
                },
                viewModel = gutenbergViewModel
            )
        }

        composable(Screens.AllBooks.route) {
            AllBooksScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { book ->
                    navController.navigate(Screens.BookDetailsScreen.createRoute(bookId = book.id))
                }
            )
        }

        composable(Screens.StandardEbooks.route) {
            val standardViewModel: StandardViewModel = koinViewModel()

            StandardScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateById = { bookId ->
                    navController.navigate(Screens.BookDetailsScreen.createRoute(bookId = bookId))
                },
                onNavigateByContent = { book ->
                    navController.navigate(Screens.BookDetailsScreen.createRoute(book = book))
                },

                standardViewModel = standardViewModel
            )
        }

        composable(
            route = Screens.BookDetailsScreen.routeWithArgs,
            arguments = Screens.BookDetailsScreen.arguments
        ) {
            val bookDetailsViewModel: BookDetailsViewModel = koinViewModel()

            BookDetailsScreen(
                onBackClick = { navController.popBackStack() },
                onReadClick = { bookId, chapterHref ->
                    context.startActivity(
                        ReaderActivity.createIntent(context, bookId, chapterHref)
                    )
                },
                // ADDED: Navigation to Aira Chat
                onAiraClick = { navController.navigate(Screens.Aira.route) },
                bookDetailsViewModel = bookDetailsViewModel
            )
        }

        composable("all_books") {
            AllBooksScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { book ->
                    navController.navigate(Screens.BookDetailsScreen.createRoute(bookId = book.id))
                }
            )
        }

        composable(Screens.Appearance.route) {
            val themeViewModel: ThemeViewModel = koinViewModel()

            AppearanceScreen(
                onNavigateBack = { navController.popBackStack() },
                themeViewModel = themeViewModel
            )
        }

        composable(Screens.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLicenses = {
                    navController.navigate(Screens.Licenses.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screens.Aira.route) {
            AiraChatScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screens.Licenses.route) {
            AttributionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screens.Storage.route) {
            StorageScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screens.ManageCategories.route) {
            val categoryViewModel: CategoryViewModel = koinViewModel()

            CategoryScreen(
                onBack = { navController.popBackStack() },
                categoryViewModel = categoryViewModel
            )
        }
    }
}