package com.yugentech.quill.ui.dash.screens.bookDetailsScreen.components

// Helper Functions
fun cleanDescription(description: String?): String {
    if (description.isNullOrBlank()) return "No description available."
    return description.replace(Regex("<a\\b[^>]*>|</a>"), "").trim()
}
