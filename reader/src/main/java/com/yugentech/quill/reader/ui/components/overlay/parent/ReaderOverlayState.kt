package com.yugentech.quill.reader.ui.components.overlay.parent

data class ReaderOverlayState(
    val bookTitle: String,
    val chapterTitle: String,
    val chapterPagesLeft: Int,
    val progress: Float,
    val totalPages: Int,
    val currentChapterIndex: Int = 0,
    val selectedText: String? = null
)