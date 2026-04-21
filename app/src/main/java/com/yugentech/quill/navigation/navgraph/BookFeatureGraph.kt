package com.yugentech.quill.navigation.navgraph

import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.ui.shared.airaChat.parent.AiraChatScreen
import com.yugentech.quill.ui.shared.bookDetails.parent.BookDetailsScreen
import com.yugentech.quill.ui.shared.bookDetails.parent.NotesScreen
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
                context.startActivity(ReaderActivity.createIntent(context, bookId, chapterHref))
            },
            onAiraClick = { bookId ->
                navController.navigate(AppScreen.Aira.route + "/$bookId") {
                    launchSingleTop = true
                }
            },
            onNotesClick = { bookId ->
                navController.navigate(AppScreen.NotesScreen.route + "/$bookId") {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(
        route = AppScreen.NotesScreen.route + "/{bookId}", // 1. Added the placeholder here
        arguments = listOf(navArgument("bookId") { type = NavType.StringType }) // 2. Declared the argument
    ) { backStackEntry ->
        val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable

        NotesScreen(
            bookId = bookId,
            onBackClick = { navController.popBackStack() }
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