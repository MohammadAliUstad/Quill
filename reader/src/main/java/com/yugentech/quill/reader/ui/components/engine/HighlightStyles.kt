package com.yugentech.quill.reader.ui.components.engine

import android.os.Parcelable
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import org.readium.r2.navigator.Decoration

@Parcelize
data class UnderlineStyle(
    @ColorInt override val tint: Int,
    override val isActive: Boolean = false,
) : Decoration.Style, Decoration.Style.Tinted, Decoration.Style.Activable

