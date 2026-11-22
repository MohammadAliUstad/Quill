package com.yugentech.quill.navigation.navgraph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.sources.discover.viewmodel.ShelfViewModel
import com.yugentech.quill.ui.shelf.parent.ShelfScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun NavGraphBuilder.sourceGraph(
    navController: NavHostController
) {
    composable(
        route = AppScreen.Shelf.ROUTE,
        arguments = listOf(navArgument(AppScreen.Shelf.ARG) { type = NavType.StringType })
    ) { backStackEntry ->
        val shelf = backStackEntry.arguments?.getString(AppScreen.Shelf.ARG)
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
            .orEmpty()
        val shelfViewModel: ShelfViewModel = koinViewModel(parameters = { parametersOf(shelf) })
        ShelfScreen(
            viewModel = shelfViewModel,
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
}
