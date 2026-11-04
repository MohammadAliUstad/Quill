package com.yugentech.quill.ui.access.signUp

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
import com.yugentech.quill.ui.access.signIn.components.StackingBooks
import com.yugentech.quill.ui.access.signIn.components.AuthSwitchLink
import com.yugentech.quill.ui.access.signIn.components.forms.SignUpForm
import com.yugentech.quill.ui.main.components.ToastMessage
import com.yugentech.theme.tokens.spacing

/** Begin your story, under a pile of first books building up: laid out as Hajime's sign-up. */
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onSignUp: (name: String, email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    AuthScaffold(
        header = { StackingBooks() },
        greeting = "Begin your story",
        accent = "once upon a time, a first book…",
        subtitle = "Experience the best of e-reading.",
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
            }
        }
    ) {
        SignUpForm(
            isLoading = authState.isLoading,
            onSignUp = onSignUp,
            onGoogleSignIn = onGoogleSignIn,
            onClearError = { authViewModel.clearError() }
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.m))

        AuthSwitchLink(
            prompt = "Already reading with us?",
            action = "Sign in",
            onClick = onNavigateToSignIn
        )
    }
}
