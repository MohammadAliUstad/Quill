package com.yugentech.quill.ui.auth.signInScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.yugentech.quill.R
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.ui.auth.components.dialog.ForgotPasswordSuccessDialog
import com.yugentech.quill.ui.mainScreen.components.ToastMessage
import com.yugentech.quill.ui.auth.components.forms.SignInForm
import com.yugentech.quill.ui.auth.state.ForgotPasswordState
import com.yugentech.theme.tokens.components
import com.yugentech.theme.tokens.spacing

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onSignIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val forgotPasswordState by authViewModel.forgotPasswordState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.book_in))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = MaterialTheme.colorScheme.primary.toArgb(),
            keyPath = arrayOf("**")
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // THE FIX: safeDrawing automatically handles the keyboard and navigation bars perfectly
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(scrollState)
                    .padding(horizontal = MaterialTheme.spacing.m),
                horizontalAlignment = Alignment.CenterHorizontally,
                // THE FIX: Removed Arrangement.Center so the layout doesn't jump unnecessarily
            ) {
                // THE FIX: Use a fixed responsive spacer to push content down to the visual center
                Spacer(modifier = Modifier.height(screenHeight * 0.12f))

                Box(
                    modifier = Modifier
                        .padding(vertical = MaterialTheme.spacing.s)
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        dynamicProperties = dynamicProperties,
                        modifier = Modifier.size(MaterialTheme.components.imageSizeSmall)
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

                Text(
                    text = "Quill",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

                Text(
                    text = "Ready for the next chapter?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.l))

                SignInForm(
                    isLoading = authState.isLoading,
                    onClearError = { authViewModel.clearError() },
                    onSignIn = onSignIn,
                    onGoogleSignIn = onGoogleSignIn,
                    onForgotPassword = onForgotPassword
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                TextButton(
                    onClick = onNavigateToSignUp,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs)
                ) {
                    Text(
                        text = "Don't have an account? Sign Up",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.l))
            }

            ToastMessage(
                message = authState.error,
                onDismiss = { authViewModel.clearError() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .zIndex(1f)
            )

            if (forgotPasswordState is ForgotPasswordState.Error) {
                val errorMsg = (forgotPasswordState as ForgotPasswordState.Error).message
                ToastMessage(
                    message = errorMsg,
                    onDismiss = { authViewModel.clearForgotPasswordState() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .zIndex(1f)
                )
            }
        }

        if (forgotPasswordState is ForgotPasswordState.Success) {
            ForgotPasswordSuccessDialog(
                onDismiss = { authViewModel.clearForgotPasswordState() }
            )
        }
    }
}