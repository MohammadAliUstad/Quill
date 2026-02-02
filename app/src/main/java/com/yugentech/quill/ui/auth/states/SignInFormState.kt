package com.yugentech.quill.ui.auth.states

import com.yugentech.quill.utils.AppConstants.EMPTY_STRING

data class SignInFormState(
    val email: String = EMPTY_STRING,
    val password: String = EMPTY_STRING,
    val emailError: String = EMPTY_STRING,
    val passwordError: String = EMPTY_STRING
)