package com.yugentech.quill.navigation.navgraph

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.billing.SubscriptionViewModel
import com.yugentech.quill.bookDetails.viewmodel.BookDetailsViewModel
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.storage.StorageViewModel
import com.yugentech.quill.ui.more.aboutAira.AiraAboutScreen
import com.yugentech.quill.ui.more.aboutScreen.parent.AboutScreen
import com.yugentech.quill.ui.more.appearanceScreen.parent.AppearanceScreen
import com.yugentech.quill.ui.more.attributions.parent.AttributionsScreen
import com.yugentech.quill.ui.more.categoryScreen.parent.CategoryScreen
import com.yugentech.quill.ui.more.contributorsScreen.ContributorsScreen
import com.yugentech.quill.ui.more.editProfileScreen.EditProfileScreen
import com.yugentech.quill.ui.more.insightsScreen.InsightsScreen
import com.yugentech.quill.ui.more.insightsScreen.insights.InsightsViewModel
import com.yugentech.quill.ui.more.storageScreen.parent.StorageScreen
import com.yugentech.quill.ui.more.subscriptions.parent.SubscriptionsScreen
import com.yugentech.quill.ui.shared.airaScreen.parent.AiraChatScreen
import com.yugentech.quill.ui.shared.bookDetailsScreen.parent.BookDetailsScreen
import com.yugentech.quill.user.viewmodel.UserViewModel
import com.yugentech.theme.ThemeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

fun NavGraphBuilder.detailGraph(
    navController: NavHostController,
    context: Context,
    authViewModel: AuthViewModel,
    subscriptionViewModel: SubscriptionViewModel
) {

    composable(
        route = AppScreen.BookDetailsScreen.routeWithArgs,
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
            }
        )
    }

    composable(AppScreen.Insights.route) {
        val viewModel: InsightsViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        InsightsScreen(
            uiState = uiState,
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.Subscriptions.route) {
        SubscriptionsScreen(
            onBack = {
                navController.popBackStack()
            },
            subscriptionViewModel = subscriptionViewModel
        )
    }

    composable(AppScreen.EditProfileScreen.route) {
        val userViewModel: UserViewModel = koinViewModel()
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        val currentUserId = authState.userId

        if (currentUserId != null) {
            EditProfileScreen(
                userViewModel = userViewModel,
                userId = currentUserId,
                onNavigateBack = { navController.popBackStack() }
            )
        } else {
            Timber.w("Navigated to EditProfile without valid User ID")
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }

    composable(
        route = AppScreen.Aira.route + "/{bookId}",
        arguments = listOf(navArgument("bookId") { type = NavType.StringType })
    ) { backStackEntry ->
        val bookId = backStackEntry.arguments?.getString("bookId") ?: ""

        val airaViewModel: AiraViewModel = koinViewModel(
            parameters = { parametersOf(bookId) }
        )

        AiraChatScreen(
            viewModel = airaViewModel,
            onBackClick = { navController.popBackStack() },
            onNavigateToSubscriptions = {
                navController.navigate(AppScreen.Subscriptions.route) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppScreen.Appearance.route) {
        val themeViewModel: ThemeViewModel = koinViewModel()
        AppearanceScreen(
            themeViewModel = themeViewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.AboutAira.route) {
        AiraAboutScreen(
            onBack = {
                navController.popBackStack()
            }
        )
    }

    composable(AppScreen.Contributors.route) {
        ContributorsScreen(
            onBack = {
                navController.popBackStack()
            }
        )
    }

    composable(AppScreen.ManageCategories.route) {
        val categoryViewModel: CategoryViewModel = koinViewModel()
        CategoryScreen(
            categoryViewModel = categoryViewModel,
            onBack = { navController.popBackStack() }
        )
    }

    composable(AppScreen.Storage.route) {
        val storageViewModel: StorageViewModel = koinViewModel()
        StorageScreen(
            storageViewModel = storageViewModel,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(AppScreen.About.route) {
        AboutScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToLicenses = {
                navController.navigate(AppScreen.Licenses.route) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(AppScreen.Licenses.route) {
        AttributionsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}