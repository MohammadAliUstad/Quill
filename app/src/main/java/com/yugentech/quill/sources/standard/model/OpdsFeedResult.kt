package com.yugentech.quill.sources.standard.model

import com.yugentech.quill.database.model.Book

data class OpdsFeedResult(
    val books: List<Book>,
    val nextPageUrl: String?
)