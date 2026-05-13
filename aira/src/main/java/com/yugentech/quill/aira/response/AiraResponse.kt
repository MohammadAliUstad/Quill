package com.yugentech.quill.aira.response

import com.yugentech.quill.database.model.RetrievedChunk

sealed class AiraResponse {
    data class Success(
        val text: String,
        val sources: List<RetrievedChunk> = emptyList()
    ) : AiraResponse()

    data class Error(val message: String) : AiraResponse()
}