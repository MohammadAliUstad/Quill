package com.yugentech.quill.ui.auth.signUpScreen

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
import androidx.compose.runtime.collectAsState
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
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.yugentech.quill.R
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.ui.mainScreen.components.ToastMessage
import com.yugentech.quill.ui.auth.components.forms.SignUpForm
import com.yugentech.theme.tokens.components
import com.yugentech.theme.tokens.spacing

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onSignUp: (name: String, email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.book_morph))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = MaterialTheme.colorScheme.primary.toArgb(),
            keyPath = arrayOf("**")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(scrollState)
                    .padding(horizontal = MaterialTheme.spacing.m),
                horizontalAlignment = Alignment.CenterHorizontally,
                // Removed Arrangement.Center
            ) {
                // Responsive top spacer
                Spacer(modifier = Modifier.height(screenHeight * 0.08f))

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

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.l))

                Text(
                    text = "Begin your story",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

                Text(
                    text = "Experience the best of e-reading.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.l))

                SignUpForm(
                    isLoading = authState.isLoading,
                    onSignUp = onSignUp,
                    onGoogleSignIn = onGoogleSignIn,
                    onClearError = { authViewModel.clearError() }
                )

                TextButton(
                    onClick = onNavigateToSignIn,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.have_an_account),
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
        }
    }
}