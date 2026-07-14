package com.yugentech.quill.navigation.navgraph

import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.ui.shared.airaChat.parent.AiraChatScreen
import com.yugentech.quill.ui.shared.airaChat.viewmodel.AiraViewModel
import com.yugentech.quill.ui.shared.bookDetails.parent.BookDetailsScreen
import com.yugentech.quill.ui.shared.bookDetails.parent.HighlightsScreen
import com.yugentech.quill.ui.shared.bookDetails.parent.HighlightsViewModel
import com.yugentech.theme.tokens.AppConstants.EMPTY
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.bookFeatureGraph(
    navController: NavHostController,
    context: Context
) {
    composable(
        route = AppScreen.BookDetailsScreen.ROUTE,
        arguments = AppScreen.BookDetailsScreen.arguments
    ) {
        val bookDetailsViewModel: BookDetailsViewModel = koinViewModel()
        BookDetailsScreen(
            bookDetailsViewModel = bookDetailsViewModel,
            onBackClick = { navController.popBackStack() },
            onReadClick = { bookId, chapterHref ->
                context.startActivity(
                    ReaderActivity.createIntent(
                        context = context,
                        bookId = bookId,
                        initialChapterHref = chapterHref
                    )
                )
            },
            onAiraClick = { bookId ->
                navController.navigate(AppScreen.Aira.route + "/$bookId") {
                    launchSingleTop = true
                }
            },
            onHighlightsClick = { bookId ->
                navController.navigate(AppScreen.HighlightsScreen.route + "/$bookId") {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(
        route = AppScreen.HighlightsScreen.route + "/{bookId}",
        arguments = listOf(navArgument("bookId") {
            type = NavType.StringType
        })
    ) { backStackEntry ->
        val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
        val highlightsViewModel: HighlightsViewModel = koinViewModel()

        HighlightsScreen(
            bookId = bookId,
            onBackClick = { navController.popBackStack() },
            onHighlightClick = { clickedBookId, locatorJson ->
                context.startActivity(
                    ReaderActivity.createIntent(
                        context = context,
                        bookId = clickedBookId,
                        locatorJson = locatorJson
                    )
                )
            },
            viewModel = highlightsViewModel
        )
    }

    composable(
        route = AppScreen.Aira.route + "/{bookId}",
        arguments = listOf(navArgument("bookId") { type = NavType.StringType })
    ) { backStackEntry ->
        val bookId = backStackEntry.arguments?.getString("bookId") ?: EMPTY

        val airaViewModel: AiraViewModel = koinViewModel(
            parameters = { parametersOf(bookId) }
        )

        AiraChatScreen(
            viewModel = airaViewModel,
            onBackClick = { navController.popBackStack() },
            navigateToSubscriptions = {
                navController.navigate(AppScreen.Subscriptions.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}