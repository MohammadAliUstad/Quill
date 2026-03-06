package com.yugentech.quill.utils

sealed class ImportResult {
    data class Success(val bookId: String, val title: String) : ImportResult()
    data class Failure(val fileName: String, val reason: String) : ImportResult()
}