package com.yugentech.quill.reader.ui.components.engine

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.fragment.app.FragmentContainerView
import org.readium.r2.shared.publication.Locator
import kotlin.math.abs

/** How far sideways (dp) a swipe must travel to change the chapter in scroll mode. */
private const val MIN_CHAPTER_SWIPE_DP = 72f

/** How many times its vertical travel a chapter swipe's sideways travel must be. */
private const val SWIPE_DOMINANCE = 1.5f

class ReadiumWrapperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onAskAira: (String, Locator?) -> Unit = { _, _ -> }
    var onHighlightRequest: (Locator) -> Unit = {}
    var onSelectionStarted: () -> Unit = {}
    var onSelectionEnded: () -> Unit = {}
    var onSelectionChanged: (text: String) -> Unit = {}
    var currentSelectedText: String? = null
    var currentSelectionLocator: Locator? = null
    var isPro: Boolean = false
    var isAiraReady: Boolean = false

    /** Whether the book is in scroll mode, where sideways swipes change the chapter. */
    var scrollMode: Boolean = false

    private var activeActionMode: ActionMode? = null
    val container = FragmentContainerView(context).also { addView(it) }

    // Where the current gesture went down, and whether it's a single-finger one being watched
    private var downX = 0f
    private var downY = 0f
    private var trackingSwipe = false
    private var multiTouch = false
    private val minSwipeDistancePx = MIN_CHAPTER_SWIPE_DP * resources.displayMetrics.density

    /**
     * In scroll mode Readium changes the chapter on any gesture that drifted sideways past the
     * touch slop (about 16dp) while moving less than 200 raw pixels up or down: a short scroll
     * that wandered a little sideways flipped the chapter. Readium decides by comparing where
     * the finger lifted with where it went down, so a gesture that isn't a real sideways swipe
     * gets its lift reported at the x it started from. Readium then sees no sideways travel and
     * stays put. Vertical scrolling and flings are untouched (they ride the vertical motion).
     *
     * A real swipe has to travel [MIN_CHAPTER_SWIPE_DP] sideways and be mostly sideways
     * ([SWIPE_DOMINANCE] times its vertical travel). Gestures while text is selected (dragging
     * a handle) and multi-finger ones are left exactly as they are, so handles land where
     * they're let go.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!scrollMode) return super.dispatchTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                trackingSwipe = true
                multiTouch = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> multiTouch = true
            MotionEvent.ACTION_CANCEL -> trackingSwipe = false
            MotionEvent.ACTION_UP -> if (trackingSwipe) {
                trackingSwipe = false
                if (!multiTouch && activeActionMode == null) {
                    val dx = abs(ev.x - downX)
                    val dy = abs(ev.y - downY)
                    val deliberateSwipe = dx >= minSwipeDistancePx && dx >= dy * SWIPE_DOMINANCE
                    if (!deliberateSwipe && ev.x != downX) {
                        val neutral = MotionEvent.obtain(ev).apply { offsetLocation(downX - ev.x, 0f) }
                        try {
                            return super.dispatchTouchEvent(neutral)
                        } finally {
                            neutral.recycle()
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

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
