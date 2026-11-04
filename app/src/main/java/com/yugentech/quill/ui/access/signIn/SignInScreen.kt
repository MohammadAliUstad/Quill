package com.yugentech.quill.ui.access.signIn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.auth.viewmodel.AuthViewModel
import com.yugentech.quill.ui.access.signIn.components.AuthScaffold
import com.yugentech.quill.ui.access.signIn.components.BookmarkedPage
import com.yugentech.quill.ui.access.signIn.components.AuthSwitchLink
import com.yugentech.quill.ui.access.signIn.components.dialog.ForgotPasswordSuccessDialog
import com.yugentech.quill.ui.access.signIn.components.forms.SignInForm
import com.yugentech.quill.ui.access.signIn.state.ForgotPasswordState
import com.yugentech.quill.ui.main.components.ToastMessage
import com.yugentech.theme.tokens.spacing

/** Welcome back, under a book held open at its bookmark: laid out as Hajime's sign-in. */
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

    AuthScaffold(
        header = { BookmarkedPage() },
        greeting = "Welcome back",
        accent = "your bookmark is right where you left it",
        subtitle = "Ready for the next chapter?",
        overlay = {
            Box(modifier = Modifier.fillMaxSize()) {
                ToastMessage(
                    message = authState.error,
                    onDismiss = { authViewModel.clearError() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .zIndex(1f)
                )

                (forgotPasswordState as? ForgotPasswordState.Error)?.let { error ->
                    ToastMessage(
                        message = error.message,
                        onDismiss = { authViewModel.clearForgotPasswordState() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .zIndex(1f)
                    )
                }
            }
        }
    ) {
        SignInForm(
            isLoading = authState.isLoading,
            onClearError = { authViewModel.clearError() },
            onSignIn = onSignIn,
            onGoogleSignIn = onGoogleSignIn,
            onForgotPassword = onForgotPassword
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

        AuthSwitchLink(
            prompt = "New to Quill?",
            action = "Create an account",
            onClick = onNavigateToSignUp
        )
    }

    if (forgotPasswordState is ForgotPasswordState.Success) {
        ForgotPasswordSuccessDialog(
            onDismiss = { authViewModel.clearForgotPasswordState() }
        )
    }
}
