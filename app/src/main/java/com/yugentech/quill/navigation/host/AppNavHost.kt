package com.yugentech.quill.navigation.host

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yugentech.quill.auth.state.AuthState
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.billing.viewmodel.SubscriptionViewModel
import com.yugentech.quill.navigation.navgraph.aboutGraph
import com.yugentech.quill.navigation.navgraph.accountGraph
import com.yugentech.quill.navigation.navgraph.authGraph
import com.yugentech.quill.navigation.navgraph.bookFeatureGraph
import com.yugentech.quill.navigation.navgraph.mainGraph
import com.yugentech.quill.navigation.navgraph.settingsGraph
import com.yugentech.quill.navigation.navgraph.sourceGraph
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.access.onboarding.OnboardingScreen
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    webClientId: String,
    authViewModel: AuthViewModel,
    showOnboarding: Boolean,
    onOnboardingComplete: () -> Unit,
    shouldNavigateToHome: Boolean = false,
    onNavigatedToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val subscriptionViewModel: SubscriptionViewModel = koinViewModel()

    val startDestination = remember {
        when {
            showOnboarding -> AppScreen.Onboarding.route
            authState.isUserLoggedIn && authState.userId != null -> AppScreen.Main.route
            else -> AppScreen.SignIn.route
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Timber.d("Google Sign-In Activity Result OK")
                authViewModel.handleGoogleSignInResult(result.data)
            } else {
                Timber.w("Google Sign-In Activity Result Cancelled or Failed")
            }
        }
    )

    LaunchedEffect(authState.intent) {
        authState.intent?.let {
            Timber.d("Launching Google Sign-In Intent")
            launcher.launch(IntentSenderRequest.Builder(it).build())
        }
    }

    LaunchedEffect(authState.isUserLoggedIn, authState.userId, showOnboarding) {
        if (!authState.isLoading && !authState.isInitializing) {
            val currentRoute = navController.currentDestination?.route

            when {
                showOnboarding -> {
                    if (currentRoute != AppScreen.Onboarding.route) {
                        navController.navigate(AppScreen.Onboarding.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                authState.isUserLoggedIn && authState.userId != null -> {
                    if (currentRoute != AppScreen.Main.route) {
                        navController.navigate(AppScreen.Main.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                else -> {
                    if (currentRoute != AppScreen.SignIn.route && currentRoute != AppScreen.SignUp.route) {
                        navController.navigate(AppScreen.SignIn.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(shouldNavigateToHome) {
        if (shouldNavigateToHome) {
            Timber.d("Popping back to Main screen")
            navController.popBackStack(AppScreen.Main.route, inclusive = false)
            onNavigatedToHome()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(250),
                initialOffsetX = { 1000 }
            ) + fadeIn(animationSpec = tween(250))
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(250),
                targetOffsetX = { -1000 }
            ) + fadeOut(animationSpec = tween(250))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(250),
                initialOffsetX = { -1000 }
            ) + fadeIn(animationSpec = tween(250))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(250),
                targetOffsetX = { 1000 }
            ) + fadeOut(animationSpec = tween(250))
        }
    ) {
        composable(AppScreen.Onboarding.route) {
            OnboardingScreen(
                onFinish = onOnboardingComplete
            )
        }

        authGraph(
            navController = navController,
            authViewModel = authViewModel,
            webClientId = webClientId,
            context = context
        )
        mainGraph(
            navController = navController,
            context = context
        )
        sourceGraph(
            navController = navController
        )
        bookFeatureGraph(navController, context)
        accountGraph(navController, authViewModel, subscriptionViewModel)
        settingsGraph(navController)
        aboutGraph(navController)
    }
}