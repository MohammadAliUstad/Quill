package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.sources.gutenberg.parent.GutenbergScreen
import com.yugentech.quill.ui.sources.standard.parent.StandardScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.sourceGraph(
    navController: NavHostController
) {
    composable(AppScreen.StandardEbooks.route) {
        val standardViewModel: com.yugentech.quill.sources.standard.viewmodel.StandardViewModel = koinViewModel()
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
        val gutenbergViewModel: com.yugentech.quill.sources.gutenberg.viewmodel.GutenbergViewModel = koinViewModel()
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