package com.yugentech.quill.reader.ui.components.engine

import android.content.Context
import android.util.AttributeSet
import android.view.ActionMode
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.fragment.app.FragmentContainerView
import org.readium.r2.shared.publication.Locator
import timber.log.Timber
import kotlin.math.abs

/**
 * Most frames in a row the selection page lock will skip while putting the page back. A stray
 * reveal needs one; this only stops a stubborn scroll from holding up drawing.
 */
private const val MAX_SKIPPED_FRAMES = 3

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

        val generation = ++selectionGeneration
        if (!scrollMode && webView != null) lockPageScroll(webView)

        onSelectionStarted()

        val onDestroy: () -> Unit = {
            // Restore hardware rendering once the selection is settled/dismissed.
            webView?.setLayerType(LAYER_TYPE_HARDWARE, null)
            // Only the newest selection's end releases the page (a restarted action mode may
            // already be holding it)
            if (generation == selectionGeneration) unlockPageScroll()
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

    // ── Page lock while text is selected (paginated mode) ─────────────────────────────────────
    //
    // Dragging the upper handle into a paragraph that began on the previous page makes Chromium
    // scroll the page back to reveal where the selection now starts. Two guards already undo
    // this: the page script's animation-frame guard, and Readium's onScrollChanged, which posts
    // a scroll back. A posted message is an ordinary one, though, and while a handle is being
    // dragged the window redraws every frame, so a redraw is nearly always pending. Android
    // holds ordinary messages back until that redraw is done (Chromium's own messages are
    // exempt, so the scroll itself gets through). The frame is drawn on the previous page first,
    // and only then is the scroll undone: a one-frame flash of the previous page.
    //
    // This guard runs inside that redraw, just before anything is drawn. If the page has moved
    // off the one the selection began on, it puts it straight back and skips that one frame
    // (the screen keeps showing the right page), so the previous page is never drawn.

    private var selectionGeneration = 0
    private var lockedWebView: WebView? = null
    private var lockedScrollX = 0
    private var correctionsInARow = 0

    private val pageLock = ViewTreeObserver.OnPreDrawListener {
        val webView = lockedWebView ?: return@OnPreDrawListener true
        if (webView.scrollX == lockedScrollX) {
            correctionsInARow = 0
            return@OnPreDrawListener true
        }
        if (correctionsInARow >= MAX_SKIPPED_FRAMES) {
            // Something keeps moving it back (not a stray reveal): don't hold up drawing any
            // longer; the older guards still have their say
            correctionsInARow = 0
            return@OnPreDrawListener true
        }
        correctionsInARow++
        Timber.d("Selection page lock: page drifted %d -> %d, restored", lockedScrollX, webView.scrollX)
        webView.scrollTo(lockedScrollX, webView.scrollY)
        false
    }

    private fun lockPageScroll(webView: WebView) {
        if (lockedWebView === webView) return // Already holding this page (action mode restarted)
        unlockPageScroll()
        lockedWebView = webView
        lockedScrollX = webView.scrollX
        correctionsInARow = 0
        viewTreeObserver.addOnPreDrawListener(pageLock)
    }

    private fun unlockPageScroll() {
        if (lockedWebView == null) return
        lockedWebView = null
        correctionsInARow = 0
        if (viewTreeObserver.isAlive) viewTreeObserver.removeOnPreDrawListener(pageLock)
    }

    override fun onDetachedFromWindow() {
        unlockPageScroll()
        super.onDetachedFromWindow()
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
