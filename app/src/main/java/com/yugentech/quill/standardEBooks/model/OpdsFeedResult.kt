package com.yugentech.quill.standardEBooks.model

import com.yugentech.quill.database.model.Book

data class OpdsFeedResult(
    val books: List<Book>,
    val nextPageUrl: String?
)