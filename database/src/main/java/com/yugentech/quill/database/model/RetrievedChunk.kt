package com.yugentech.quill.database.model

import kotlinx.serialization.Serializable

@Serializable
data class RetrievedChunk(
    val text: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val chunkIndex: Int,
    val score: Float
)