package com.yugentech.quill.navigation.navgraph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.yugentech.quill.domain.AuthRepository
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.billing.viewmodel.SubscriptionViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.info.insights.InsightsScreen
import com.yugentech.quill.insghts.viewmodel.InsightsViewModel
import com.yugentech.quill.ui.access.subscriptions.parent.GuestAiraScreen
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

        val authRepository: AuthRepository = koinInject()
        InsightsScreen(
            uiState = uiState,
            onBack = { navController.popBackStack() },
            showAira = remember { !authRepository.isGuest }
        )
    }

    composable(AppScreen.Subscriptions.route) {
        val authRepository: AuthRepository = koinInject()
        // A guest has no account for Pro to belong to: they meet Aira instead, and learn
        // what making an account brings
        if (remember { authRepository.isGuest }) {
            GuestAiraScreen(onBack = { navController.popBackStack() })
        } else {
            SubscriptionsScreen(
                onBack = { navController.popBackStack() },
                subscriptionViewModel = subscriptionViewModel
            )
        }
    }
}
