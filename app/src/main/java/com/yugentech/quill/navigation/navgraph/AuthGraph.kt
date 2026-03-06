package com.yugentech.quill.navigation.navgraph

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.mainScreen.utils.defaultEnterTransition
import com.yugentech.quill.ui.mainScreen.utils.defaultExitTransition
import com.yugentech.quill.ui.mainScreen.utils.defaultPopEnterTransition
import com.yugentech.quill.ui.mainScreen.utils.defaultPopExitTransition
import com.yugentech.quill.ui.auth.signInScreen.SignInScreen
import com.yugentech.quill.ui.auth.signUpScreen.SignUpScreen
import timber.log.Timber

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    webClientId: String,
    context: Context
) {
    // Defines the sign-in screen
    composable(
        route = AppScreen.SignIn.route,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        Timber.v("Composing SignIn Screen")
        BackHandler { (context as? Activity)?.finish() }

        SignInScreen(
            authViewModel = authViewModel,
            onSignIn = { email, password ->
                authViewModel.signIn(email, password)
            },
            onGoogleSignIn = {
                authViewModel.getGoogleSignInIntent(webClientId)
            },
            onNavigateToSignUp = {
                navController.navigate(AppScreen.SignUp.route) {
                    launchSingleTop = true
                }
            },
            onForgotPassword = { email ->
                authViewModel.forgotPassword(email)
            }
        )
    }

    // Defines the sign-up screen
    composable(
        route = AppScreen.SignUp.route,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        Timber.v("Composing SignUp Screen")
        BackHandler { navController.popBackStack() }

        SignUpScreen(
            authViewModel = authViewModel,
            onSignUp = { name, email, password ->
                authViewModel.signUp(name, email, password)
            },
            onGoogleSignIn = {
                authViewModel.getGoogleSignInIntent(webClientId)
            },
            onNavigateToSignIn = {
                navController.popBackStack()
            }
        )
    }
}