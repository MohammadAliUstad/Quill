package com.yugentech.quill.reader.ui.components.engine

import org.readium.r2.shared.publication.Locator

data class SelectionInfo(
    val text: String,
    val locator: Locator? = null,
    val rectTop: Float = 0f,
    val rectBottom: Float = 0f
)
