package com.yugentech.quill.reader.ui.components.engine

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.fragment.app.FragmentContainerView
import org.readium.r2.shared.publication.Locator

class ReadiumWrapperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onAskAira: (String) -> Unit = {}
    var onHighlightRequest: (Locator) -> Unit = {}
    var onSelectionStarted: () -> Unit = {}
    var onSelectionEnded: () -> Unit = {}
    var onSelectionChanged: (text: String) -> Unit = {}
    var currentSelectedText: String? = null
    var currentSelectionLocator: Locator? = null
    var isPro: Boolean = false
    var isAiraReady: Boolean = false
    private var activeActionMode: ActionMode? = null
    val container = FragmentContainerView(context).also { addView(it) }

    fun finishActionMode() {
        activeActionMode?.finish()
        activeActionMode = null
    }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int
    ): ActionMode? {
        // Toggle software rendering to eliminate selection flickering.
        // GPU tile rasterization can lag behind selection handle movement;
        // software mode renders atomically to a single CPU bitmap.
        val webView = (originalView as? WebView) ?: findWebView(this)
        webView?.setLayerType(LAYER_TYPE_SOFTWARE, null)

        onSelectionStarted()

        val onDestroy: () -> Unit = {
            // Restore hardware rendering once the selection is settled/dismissed.
            webView?.setLayerType(LAYER_TYPE_HARDWARE, null)
            onSelectionEnded()
        }

        val wrapped = if (callback is ActionMode.Callback2) {
            WrappedCallback2(
                callback, onAskAira, onHighlightRequest, isPro, isAiraReady,
                { currentSelectedText }, { currentSelectionLocator }, onDestroy
            )
        } else {
            WrappedCallback(
                callback, onAskAira, onHighlightRequest, isPro, isAiraReady,
                { currentSelectedText }, { currentSelectionLocator }, onDestroy
            )
        }
        val mode = super.startActionModeForChild(originalView, wrapped, type)
        activeActionMode = mode
        return mode
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWebView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
