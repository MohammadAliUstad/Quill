package com.yugentech.quill.reader.ui.components.engine

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.size
import com.yugentech.theme.tokens.AppConstants.ASKAIRA
import com.yugentech.theme.tokens.AppConstants.HIGHLIGHT
import org.readium.r2.shared.publication.Locator

class WrappedCallback(
    private val original: ActionMode.Callback,
    private val onAskAira: (String) -> Unit,
    private val onHighlightRequest: (Locator) -> Unit,
    private val isPro: Boolean,
    private val isAiraReady: Boolean,
    private val getSelectedText: () -> String?,
    private val getSelectionLocator: () -> Locator?,
    private val onDestroy: () -> Unit = {}
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Do NOT call original — that would populate Copy/Share/etc into the native menu.
        // Starting with an empty menu means FloatingActionMode never renders its floating
        // panel (no items = nothing to show), while the ActionMode itself stays alive so
        // the system-drawn selection handles remain visible. Our Compose toolbar handles
        // all actions instead.
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Menu stays empty; return false so the framework skips its invalidation pass.
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            HIGHLIGHT -> {
                getSelectionLocator()?.let { locator ->
                    onHighlightRequest(locator)
                }
                mode.finish()
                return true
            }

            ASKAIRA -> {
                onAskAira(getSelectedText() ?: "Processing selection...")
                mode.finish()
                return true
            }
        }
        return original.onActionItemClicked(mode, item)
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        original.onDestroyActionMode(mode)
        onDestroy()
    }
}

class WrappedCallback2(
    private val original: ActionMode.Callback2,
    onAskAira: (String) -> Unit,
    onHighlightRequest: (Locator) -> Unit,
    isPro: Boolean,
    isAiraReady: Boolean,
    getSelectedText: () -> String?,
    getSelectionLocator: () -> Locator?,
    onDestroy: () -> Unit = {}
) : ActionMode.Callback2() {

    private val delegate = WrappedCallback(
        original,
        onAskAira,
        onHighlightRequest,
        isPro,
        isAiraReady,
        getSelectedText,
        getSelectionLocator,
        onDestroy
    )

    override fun onCreateActionMode(mode: ActionMode, menu: Menu) =
        delegate.onCreateActionMode(mode, menu)

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) =
        delegate.onPrepareActionMode(mode, menu)

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) =
        delegate.onActionItemClicked(mode, item)

    override fun onDestroyActionMode(mode: ActionMode) =
        delegate.onDestroyActionMode(mode)

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
        original.onGetContentRect(mode, view, outRect)
        // For continuation paragraphs the anchor sits in the previous CSS column
        // (scrollX ≈ -viewWidth), giving a rect that starts far off-screen to the
        // left. Android's floating ActionMode won't show in that case. Clamp the
        // rect to the view bounds so the toolbar always stays visible.
        outRect.left = outRect.left.coerceAtLeast(0)
        outRect.top = outRect.top.coerceAtLeast(0)
        outRect.right = outRect.right.coerceIn(outRect.left + 1, view.width.coerceAtLeast(1))
        outRect.bottom = outRect.bottom.coerceIn(outRect.top + 1, view.height.coerceAtLeast(1))
    }
}