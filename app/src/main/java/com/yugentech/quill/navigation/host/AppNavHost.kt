package com.yugentech.quill.navigation.host

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yugentech.quill.auth.state.AuthState
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.billing.viewmodel.SubscriptionViewModel
import com.yugentech.quill.theme.viewmodel.ThemeViewModel
import com.yugentech.quill.navigation.navgraph.aboutGraph
import com.yugentech.quill.navigation.navgraph.accountGraph
import com.yugentech.quill.navigation.navgraph.authGraph
import com.yugentech.quill.navigation.navgraph.bookFeatureGraph
import com.yugentech.quill.navigation.navgraph.mainGraph
import com.yugentech.quill.navigation.navgraph.settingsGraph
import com.yugentech.quill.navigation.navgraph.sourceGraph
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.access.onboarding.OnboardingScreen
import com.yugentech.quill.ui.access.onboarding.components.AuraBackgroundState
import com.yugentech.quill.ui.access.onboarding.components.AuraRoutes
import com.yugentech.quill.ui.access.onboarding.components.AuraTheme
import com.yugentech.quill.ui.access.onboarding.components.LocalAuraBackground
import com.yugentech.quill.ui.access.onboarding.components.QuillAuraBackground
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    webClientId: String,
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
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

    // Onboarding, sign-in and sign-up all sit on the one aura drawn here, as in Amatyaa, so
    // moving between them changes only their content: the background never jumps or shows an
    // edge, and keeps drifting while they slide over it. It fades with the switch into the app.
    val currentEntry by navController.currentBackStackEntryAsState()
    val onAura = currentEntry?.destination?.route in AuraRoutes
    val auraBackground = remember { AuraBackgroundState() }
    val transitions = rememberNavTransitions()

    Box(modifier = Modifier.fillMaxSize()) {
    AnimatedVisibility(
        visible = onAura,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        AuraTheme {
            QuillAuraBackground(
                modifier = Modifier.fillMaxSize(),
                travel = { auraBackground.travel }
            )
        }
    }

    CompositionLocalProvider(LocalAuraBackground provides auraBackground) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Material 3 motion for every screen change, as in Hajime (NavTransitions)
        enterTransition = { transitions.enter(this) },
        exitTransition = { transitions.exit(this) },
        popEnterTransition = { transitions.popEnter(this) },
        popExitTransition = { transitions.popExit(this) }
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
        settingsGraph(navController, themeViewModel)
        aboutGraph(navController)
    }
    }
    }
}