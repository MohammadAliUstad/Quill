package com.yugentech.quill.aira.response

sealed class AiraResponse {
    data class Success(
        val text: String
    ) : AiraResponse()

    data class Error(val message: String) : AiraResponse()
}
