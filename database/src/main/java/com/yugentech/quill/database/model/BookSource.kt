package com.yugentech.quill.database.model

import kotlinx.serialization.Serializable

@Serializable
enum class BookSource {
    STANDARD_EBOOKS,
    USER_IMPORTED,
    GUTENBERG
}