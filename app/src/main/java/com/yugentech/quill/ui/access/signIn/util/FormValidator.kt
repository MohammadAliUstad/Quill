package com.yugentech.quill.ui.access.signIn.util

import android.util.Patterns
import com.yugentech.theme.tokens.AppConstants.EMPTY
import com.yugentech.quill.R
import com.yugentech.quill.localization.AppStrings

object FormValidator {

    fun validateName(name: String): String {
        return when {
            name.isBlank() -> AppStrings.get(R.string.error_name_empty)
            name.length < 2 -> AppStrings.get(R.string.error_name_short)
            else -> EMPTY
        }
    }

    fun validateEmail(email: String): String {
        return when {
            email.isBlank() -> AppStrings.get(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                AppStrings.get(R.string.error_email_invalid)

            else -> EMPTY
        }
    }

    fun validatePassword(password: String): String {
        return when {
            password.isBlank() -> AppStrings.get(R.string.error_password_empty)
            password.length < 8 -> AppStrings.get(R.string.error_password_short)
            !password.any { it.isDigit() } -> AppStrings.get(R.string.error_password_number)
            !password.any { it.isUpperCase() } -> AppStrings.get(R.string.error_password_uppercase)
            !password.any { it.isLowerCase() } -> AppStrings.get(R.string.error_password_lowercase)
            else -> EMPTY
        }
    }

    fun validateSignInForm(
        email: String,
        password: String,
        onEmailError: (String) -> Unit,
        onPasswordError: (String) -> Unit
    ): Boolean {
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)

        onEmailError(emailError)
        onPasswordError(passwordError)

        return emailError.isEmpty() && passwordError.isEmpty()
    }

    fun validateSignUpForm(
        name: String,
        email: String,
        password: String,
        onNameError: (String) -> Unit,
        onEmailError: (String) -> Unit,
        onPasswordError: (String) -> Unit
    ): Boolean {
        val nameError = validateName(name)
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)

        onNameError(nameError)
        onEmailError(emailError)
        onPasswordError(passwordError)

        return nameError.isEmpty() && emailError.isEmpty() && passwordError.isEmpty()
    }
}