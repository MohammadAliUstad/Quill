package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.gutenberg.viewmodel.GutenbergViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.standardEBooks.viewmodel.StandardViewModel
import com.yugentech.quill.ui.sources.gutenberg.parent.GutenbergScreen
import com.yugentech.quill.ui.sources.standardScreen.parent.StandardScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.sourceGraph(
    navController: NavHostController
) {
    composable(AppScreen.StandardEbooks.route) {
        val standardViewModel: StandardViewModel = koinViewModel()
        StandardScreen(
            standardViewModel = standardViewModel,
            onBackClick = { navController.popBackStack() },
            onNavigateById = { bookId ->
                navController.navigate(AppScreen.BookDetailsScreen.createRoute(bookId = bookId)) {
                    launchSingleTop = true
                }
            },
            onNavigateByContent = { book ->
                navController.navigate(AppScreen.BookDetailsScreen.createRoute(book = book)) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppScreen.Gutenberg.route) {
        val gutenbergViewModel: GutenbergViewModel = koinViewModel()
        GutenbergScreen(
            viewModel = gutenbergViewModel,
            onBackClick = { navController.popBackStack() },
            onNavigateByContent = { book ->
                navController.navigate(AppScreen.BookDetailsScreen.createRoute(book = book)) {
                    launchSingleTop = true
                }
            }
        )
    }
}