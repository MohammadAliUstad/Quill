package com.yugentech.quill.reader.state

sealed interface PeekState {
    data object Idle : PeekState
    data object Loading : PeekState
    data class Response(val text: String) : PeekState
}