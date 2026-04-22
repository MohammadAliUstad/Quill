package com.yugentech.quill.reader.ui.components.engine

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentContainerView
import org.readium.r2.shared.publication.Locator

class ReadiumWrapperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onAskAira: (String) -> Unit = {}
    var onHighlightRequest: (Locator) -> Unit = {}

    var currentSelectedText: String? = null
    var currentSelectionLocator: Locator? = null
    var isPro: Boolean = false
    var isAiraReady: Boolean = false

    val container = FragmentContainerView(context).also { addView(it) }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int
    ): ActionMode? {
        val wrapped = if (callback is ActionMode.Callback2) {
            WrappedCallback2(
                callback,
                onAskAira,
                onHighlightRequest,
                { isPro },
                { isAiraReady },
                { currentSelectedText },
                { currentSelectionLocator }
            )
        } else {
            WrappedCallback(
                callback,
                onAskAira,
                onHighlightRequest,
                { isPro },
                { isAiraReady },
                { currentSelectedText },
                { currentSelectionLocator }
            )
        }
        return super.startActionModeForChild(originalView, wrapped, type)
    }
}