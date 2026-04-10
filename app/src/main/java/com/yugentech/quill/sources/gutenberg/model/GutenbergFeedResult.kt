package com.yugentech.quill.sources.gutenberg.model

import com.yugentech.quill.database.model.Book

data class GutenbergFeedResult(
    val books: List<Book>,
    val nextPageUrl: String?
)