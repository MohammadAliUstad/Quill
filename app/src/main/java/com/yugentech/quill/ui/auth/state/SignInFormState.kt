package com.yugentech.quill.ui.auth.state

import com.yugentech.theme.tokens.AppConstants.EMPTY

data class SignInFormState(
    val email: String = EMPTY,
    val password: String = EMPTY,
    val emailError: String = EMPTY,
    val passwordError: String = EMPTY
)