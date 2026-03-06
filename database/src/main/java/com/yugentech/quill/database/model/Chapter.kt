package com.yugentech.quill.database.model

import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val title: String,
    val href: String,
    val index: Int,
    val depth: Int = 0,
    val pageCount: Int = 0
)