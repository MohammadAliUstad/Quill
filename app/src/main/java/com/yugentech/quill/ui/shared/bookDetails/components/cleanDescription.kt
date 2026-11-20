package com.yugentech.quill.ui.shared.bookDetails.components

import com.yugentech.quill.R
import com.yugentech.quill.localization.AppStrings

fun cleanDescription(description: String?): String {
    if (description.isNullOrBlank()) return AppStrings.get(R.string.no_description)
    return description.replace(Regex("<a\\b[^>]*>|</a>"), "").trim()
}