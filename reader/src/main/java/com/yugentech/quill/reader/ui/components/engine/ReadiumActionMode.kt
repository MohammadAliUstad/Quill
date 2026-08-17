package com.yugentech.quill.reader.ui.components.engine

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.get
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

    override fun onCreateActionMode(mode: ActionMode, menu: Menu) =
        original.onCreateActionMode(mode, menu)

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {

        original.onPrepareActionMode(mode, menu)

        for (i in menu.size - 1 downTo 0) {
            val title = menu[i].title.toString()
            if (title.contains("read aloud", true) || title.contains("define", true)) {
                menu.removeItem(menu[i].itemId)
            }
        }

        if (menu.findItem(HIGHLIGHT) == null) {
            menu.add(Menu.NONE, HIGHLIGHT, 0, "Highlight")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        if (isAiraReady) {
            if (menu.findItem(ASKAIRA) == null) {
                menu.add(Menu.NONE, ASKAIRA, 1, "Ask Aira")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }

        return true
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

    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) =
        original.onGetContentRect(mode, view, outRect)
}