package com.yugentech.quill.library.importer

sealed class ImportResult {
    data class Success(val bookId: String, val title: String) : ImportResult()
    data class Failure(val fileName: String, val reason: String) : ImportResult()

    /** The book is already in the library: nothing was imported and its progress is untouched. */
    data class AlreadyExists(val title: String) : ImportResult()
}
