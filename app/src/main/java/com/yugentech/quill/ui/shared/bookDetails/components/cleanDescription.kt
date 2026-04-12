package com.yugentech.quill.ui.shared.bookDetails.components

fun cleanDescription(description: String?): String {
    if (description.isNullOrBlank()) return "No description available."
    return description.replace(Regex("<a\\b[^>]*>|</a>"), "").trim()
}