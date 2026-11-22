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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.yugentech.quill.ui.access.onboarding.parent.OnboardingScreen
import com.yugentech.quill.ui.access.common.components.AuraBackgroundState
import com.yugentech.quill.ui.access.common.components.AuraRoutes
import com.yugentech.quill.ui.access.common.components.AuraTheme
import com.yugentech.quill.ui.access.common.components.LocalAuraBackground
import com.yugentech.quill.ui.access.common.components.QuillAuraBackground
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

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
                authViewModel.handleGoogleSignInResult(result.data)
            } else {
                Timber.w("Google Sign-In Activity Result Cancelled or Failed")
            }
        }
    )

    LaunchedEffect(authState.intent) {
        authState.intent?.let {
            launcher.launch(IntentSenderRequest.Builder(it).build())
        }
    }

    // Set the moment onboarding is finished or skipped, so the move on to sign-in (or the app)
    // starts on the very next frame instead of after the DataStore write has landed and its
    // flow has emitted again. That round trip delayed the transition by a few frames and, if
    // auth state changed in between, could briefly route back to onboarding.
    var onboardingFinished by rememberSaveable { mutableStateOf(false) }
    val onboardingPending = showOnboarding && !onboardingFinished
    // Once the write has landed the flag has done its job; clear it so onboarding can be
    // required again later (e.g. after its stored flag is reset).
    LaunchedEffect(showOnboarding) {
        if (!showOnboarding) onboardingFinished = false
    }

    LaunchedEffect(authState.isUserLoggedIn, authState.userId, onboardingPending) {
        if (!authState.isLoading && !authState.isInitializing) {
            val currentRoute = navController.currentDestination?.route

            when {
                onboardingPending -> {
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
            navController.popBackStack(AppScreen.Main.route, inclusive = false)
            onNavigatedToHome()
        }
    }

    // Onboarding, sign-in and sign-up all sit on the one aura drawn here, as in Amatyaa, so
    // moving between them changes only their content: the background never jumps or shows an
    // edge, and stays still while they slide over it. It fades with the switch into the app.
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
            QuillAuraBackground(modifier = Modifier.fillMaxSize())
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
                onFinish = {
                    // Once only: a second tap on Skip/Start while the screen is leaving does nothing
                    if (!onboardingFinished) {
                        onboardingFinished = true
                        onOnboardingComplete()
                    }
                }
            )
        }

        // The onboarding again, from Settings: finishing, skipping or backing out returns there
        composable(AppScreen.OnboardingReplay.route) {
            OnboardingScreen(
                onFinish = { navController.popBackStack() },
                finishLabel = stringResource(R.string.onboarding_done)
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