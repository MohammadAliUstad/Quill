package com.yugentech.quill.ui.access.signIn.state

import com.yugentech.theme.tokens.AppConstants.EMPTY

data class SignUpFormState(
    val name: String = EMPTY,
    val email: String = EMPTY,
    val password: String = EMPTY,
    val confirmPassword: String = EMPTY,
    val nameError: String = EMPTY,
    val emailError: String = EMPTY,
    val passwordError: String = EMPTY,
    val confirmPasswordError: String = EMPTY
)