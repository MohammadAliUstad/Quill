package com.yugentech.quill.aira.aira

sealed class AiraResponse {
    data class Success(val text: String) : AiraResponse()
    data class Error(val message: String) : AiraResponse()
    data object IndexingNotReady : AiraResponse()
    data object NoChaptersRead : AiraResponse()
}