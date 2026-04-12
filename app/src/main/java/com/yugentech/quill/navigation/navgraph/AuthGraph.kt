package com.yugentech.quill.navigation.navgraph

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.ui.access.signIn.SignInScreen
import com.yugentech.quill.ui.access.signUp.SignUpScreen
import timber.log.Timber

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    webClientId: String,
    context: Context
) {
    composable(
        route = AppScreen.SignIn.route,
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

    composable(
        route = AppScreen.SignUp.route,
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