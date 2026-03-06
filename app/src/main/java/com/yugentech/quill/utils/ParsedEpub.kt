package com.yugentech.quill.utils

import com.yugentech.quill.database.model.Chapter

data class ParsedEpub(
    val totalPages: Int,
    val chapters: List<Chapter>
)