package com.yugentech.quill.ui.access.signIn.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yugentech.quill.ui.access.common.components.AppTextField
import com.yugentech.quill.ui.access.common.components.AuthDivider
import com.yugentech.quill.ui.access.common.components.ActionButton
import com.yugentech.quill.ui.access.common.components.GoogleSignInButton
import com.yugentech.quill.ui.access.common.components.GuestSignInButton
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.access.signIn.state.SignInFormState
import com.yugentech.quill.ui.access.common.util.FormValidator
import com.yugentech.theme.tokens.components
import com.yugentech.theme.tokens.spacing
import kotlinx.coroutines.launch
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.localization.AppStrings

/** Email and password, then Google and Guest side by side underneath. */
@Composable
fun SignInForm(
    isLoading: Boolean,
    onSignIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onClearError: () -> Unit,
    onContinueAsGuest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var formState by remember { mutableStateOf(SignInFormState()) }
    val scope = rememberCoroutineScope()
    // isLoading is shared by every sign-in path; remember which button started it so only
    // that one shows the spinner while the others are just disabled.
    var loadingFrom by rememberSaveable { mutableStateOf(LOADING_EMAIL) }

    LaunchedEffect(formState.email, formState.password) {
        onClearError()
    }

    // Starting guest mode is instant, so while it does the other buttons keep their look rather
    // than greying out for a split second before the app opens; taps are ignored instead
    val dimForLoading = isLoading && loadingFrom != LOADING_GUEST

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
    ) {
        AppTextField(
            value = formState.email,
            onValueChange = { newEmail ->
                formState = formState.copy(
                    email = newEmail,
                    emailError = FormValidator.validateEmail(newEmail)
                )
            },
            label = stringResource(R.string.label_email),
            leadingIcon = Icons.Default.Email,
            error = formState.emailError
        )

        Column {
            AppTextField(
                value = formState.password,
                onValueChange = { newPassword ->
                    formState = formState.copy(
                        password = newPassword,
                        passwordError = FormValidator.validatePassword(newPassword)
                    )
                },
                label = stringResource(R.string.label_password),
                leadingIcon = Icons.Default.Lock,
                error = formState.passwordError,
                isPassword = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        if (formState.email.isBlank()) {
                            formState = formState.copy(
                                emailError = AppStrings.get(R.string.sign_in_reset_needs_email)
                            )
                        } else {
                            onForgotPassword(formState.email)
                        }
                    },
                    modifier = Modifier.height(MaterialTheme.components.buttonSmall)
                ) {
                    Text(
                        text = stringResource(R.string.sign_in_forgot_password),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        ActionButton(
            text = stringResource(R.string.auth_sign_in),
            isLoading = isLoading && loadingFrom == LOADING_EMAIL,
            enabled = !dimForLoading,
            onClick = {
                if (isLoading) return@ActionButton
                loadingFrom = LOADING_EMAIL
                val isValid = FormValidator.validateSignInForm(
                    email = formState.email,
                    password = formState.password,
                    onEmailError = { error ->
                        formState = formState.copy(emailError = error)
                    },
                    onPasswordError = { error ->
                        formState = formState.copy(passwordError = error)
                    }
                )
                // A tap for a form that goes through, a buzz for one with something to fix
                haptic.performHapticFeedback(
                    if (isValid) HapticFeedbackType.Confirm else HapticFeedbackType.Reject
                )

                if (isValid) {
                    scope.launch {
                        onSignIn(formState.email, formState.password)
                    }
                }
            }
        )

        AuthDivider(text = stringResource(R.string.auth_divider_or))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
        ) {
            GoogleSignInButton(
                isLoading = isLoading && loadingFrom == LOADING_GOOGLE,
                enabled = !dimForLoading,
                label = stringResource(R.string.google_short),
                onClick = {
                    if (isLoading) return@GoogleSignInButton
                    loadingFrom = LOADING_GOOGLE
                    scope.launch { onGoogleSignIn() }
                },
                modifier = Modifier.weight(1f)
            )

            // Read without an account; the library stays on this phone (Settings can export it)
            GuestSignInButton(
                // Guest mode starts instantly on the phone, so no spinner: the ghost's flight
                // is the wait, and the button stays as it is until the app opens
                isLoading = false,
                enabled = !dimForLoading,
                onClick = {
                    if (isLoading) return@GuestSignInButton
                    loadingFrom = LOADING_GUEST
                    onContinueAsGuest()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private const val LOADING_EMAIL = "email"
private const val LOADING_GOOGLE = "google"
private const val LOADING_GUEST = "guest"
