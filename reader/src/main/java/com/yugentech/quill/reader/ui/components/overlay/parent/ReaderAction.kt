package com.yugentech.quill.reader.ui.components.overlay.parent

import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt

sealed interface ReaderAction {
    data object OnBackClick : ReaderAction
    data object OnSettingsClick : ReaderAction
    data object OnTocClick : ReaderAction
    data object OnSoundClick : ReaderAction
    data object OnScrubStart : ReaderAction
    data object OnScrubEnd : ReaderAction
    data object OnAskAiraClick : ReaderAction
    data object OnAiraDismiss : ReaderAction
    data object OnStopGeneration : ReaderAction
    data object OnClearSelection : ReaderAction
    data object OnSoundQuickToggle : ReaderAction
    data class OnSeek(val progress: Float) : ReaderAction
    data class OnAiraSend(val question: String) : ReaderAction
    data class OnQuickAction(val prompt: QuickPrompt) : ReaderAction
    data class OnBrightnessInteraction(val isInteracting: Boolean) : ReaderAction
}