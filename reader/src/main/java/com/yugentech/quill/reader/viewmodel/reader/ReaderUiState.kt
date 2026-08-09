package com.yugentech.quill.reader.viewmodel.reader

import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

sealed class ReaderUiState {
    data object Idle : ReaderUiState()

    data class Error(val message: String) : ReaderUiState()

    data class Success(
        val bookId: String,
        val publication: Publication,
        val totalPages: Int,
        val allPositions: List<Locator>,
        val initialLocator: Locator?
    ) : ReaderUiState()
}