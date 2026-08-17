package com.yugentech.quill.reader.ui.components.engine

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Choreographer
import android.view.MotionEvent
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
    var currentSelectedText: String? = null
    var currentSelectionLocator: Locator? = null
    var isPro: Boolean = false
    var isAiraReady: Boolean = false
    val container = FragmentContainerView(context).also { addView(it) }

    private var isSelectionActive = false
    private var lockedScrollX = 0
    private var touchDownScrollX = 0
    private var cachedWebView: WebView? = null
    private var scrollListenerAttached = false

    private val choreographer = Choreographer.getInstance()

    // Layer 3: per-frame enforcement — fires at vsync before every draw.
    // Catches any scroll the listener missed (e.g. programmatic scrolls inside Readium JS).
    private val scrollLockCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isSelectionActive) return
            resolveWebView()?.let { wv ->
                if (wv.scrollX != lockedScrollX) {
                    wv.scrollTo(lockedScrollX, wv.scrollY)
                }
            }
            choreographer.postFrameCallback(this)
        }
    }

    // Layer 1: capture the correct scroll position the instant the finger touches down,
    // before any long-press recognition or browser scroll machinery can fire.
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            resolveWebView()?.let { wv ->
                touchDownScrollX = wv.scrollX
                attachScrollListenerIfNeeded(wv)
            }
        }
        return false
    }

    // Layer 2: direct scroll listener on the WebView — fires synchronously when the
    // WebView calls onScrollChanged, before the frame is drawn. Corrects immediately.
    private fun attachScrollListenerIfNeeded(wv: WebView) {
        if (scrollListenerAttached) return
        wv.setOnScrollChangeListener { _, scrollX, scrollY, _, _ ->
            if (isSelectionActive && scrollX != lockedScrollX) {
                wv.scrollTo(lockedScrollX, scrollY)
            }
        }
        scrollListenerAttached = true
    }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int
    ): ActionMode? {
        // Use the position saved at touch-down — the browser hasn't had a chance to scroll yet.
        lockedScrollX = touchDownScrollX
        isSelectionActive = true
        onSelectionStarted()

        choreographer.removeFrameCallback(scrollLockCallback)
        choreographer.postFrameCallback(scrollLockCallback)

        val onDestroy: () -> Unit = {
            isSelectionActive = false
            choreographer.removeFrameCallback(scrollLockCallback)
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
        return super.startActionModeForChild(originalView, wrapped, type)
    }

    private fun resolveWebView(): WebView? {
        val cached = cachedWebView
        if (cached != null && cached.isAttachedToWindow) return cached
        return findDescendantWebView().also { cachedWebView = it }
    }

    private fun findDescendantWebView(): WebView? {
        fun View.find(): WebView? {
            if (this is WebView) return this
            if (this !is ViewGroup) return null
            for (i in 0 until childCount) getChildAt(i).find()?.let { return it }
            return null
        }
        return find()
    }
}
