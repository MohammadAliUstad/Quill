package com.yugentech.quill.ui.main.components

import androidx.annotation.StringRes
import com.yugentech.quill.R

enum class QuillTab(@param:StringRes val title: Int) {
    Library(R.string.tab_library),
    Discover(R.string.tab_discover),
    Sources(R.string.tab_sources),
    Settings(R.string.tab_more)
}