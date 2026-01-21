package com.yugentech.quill.ui.dash.screens

import org.readium.r2.shared.publication.Publication

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data class Error(val message: String) : ReaderUiState

    data class Success(
        // This is the core Readium object that contains the book structure
        val publication: Publication,

        // UI State
        val showControls: Boolean = false,
        val showSettings: Boolean = false,

        // We will keep your settings model for now to control fonts/themes
        val settings: ReadingSettings = ReadingSettings()
    ) : ReaderUiState
}