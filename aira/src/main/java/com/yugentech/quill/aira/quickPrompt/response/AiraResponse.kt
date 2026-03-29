package com.yugentech.quill.aira.quickPrompt.response

sealed class AiraResponse {
    data class Success(val text: String) : AiraResponse()
    data class Error(val message: String) : AiraResponse()
}