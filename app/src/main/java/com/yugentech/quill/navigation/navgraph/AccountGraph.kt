package com.yugentech.quill.navigation.navgraph

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.billing.viewmodel.SubscriptionViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.more.editProfileScreen.EditProfileScreen
import com.yugentech.quill.ui.more.insightsScreen.InsightsScreen
import com.yugentech.quill.insghts.InsightsViewModel
import com.yugentech.quill.ui.more.subscriptionsScreen.parent.SubscriptionsScreen
import com.yugentech.quill.user.viewmodel.UserViewModel
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

fun NavGraphBuilder.accountGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    subscriptionViewModel: SubscriptionViewModel
) {
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
            onBack = { navController.popBackStack() },
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
}