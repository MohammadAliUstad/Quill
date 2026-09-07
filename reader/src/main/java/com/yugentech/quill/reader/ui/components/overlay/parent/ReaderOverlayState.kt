package com.yugentech.quill.reader.ui.components.overlay.parent

import org.readium.r2.shared.publication.Locator

data class ReaderOverlayState(
    val bookTitle: String,
    val chapterTitle: String,
    val chapterPagesLeft: Int,
    val progress: Float,
    val totalPages: Int,
    val currentChapterIndex: Int = 0,
    val selectedText: String? = null,
    val selectedTextLocator: Locator? = null
)