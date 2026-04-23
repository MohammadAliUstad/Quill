package com.yugentech.quill.aira.aira.message

import com.yugentech.quill.database.model.RetrievedChunk

data class AiraMessage(
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<RetrievedChunk> = emptyList()
) {
    enum class Role {
        USER,
        AIRA
    }
}