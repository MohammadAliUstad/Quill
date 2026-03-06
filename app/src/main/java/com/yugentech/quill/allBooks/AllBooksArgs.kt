package com.yugentech.quill.allBooks

import com.yugentech.quill.database.view.LibraryBookView

object AllBooksArgs {
    var title: String = ""
    var books: List<LibraryBookView> = emptyList()
}