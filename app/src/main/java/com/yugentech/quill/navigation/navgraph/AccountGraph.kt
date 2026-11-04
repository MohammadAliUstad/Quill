package com.yugentech.quill.navigation.navgraph

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.billing.viewmodel.SubscriptionViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.info.insights.InsightsScreen
import com.yugentech.quill.insghts.viewmodel.InsightsViewModel
import com.yugentech.quill.ui.access.subscriptions.parent.SubscriptionsScreen
import org.koin.androidx.compose.koinViewModel

// The profile (name and avatar) is edited in sheets on the More screen, so there's no
// Edit Profile route here any more.
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
}
