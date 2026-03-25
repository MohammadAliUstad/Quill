package com.yugentech.quill.reader.ui.components.engine

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.yugentech.theme.tokens.AppConstants.ASKAIRA
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import androidx.core.view.size
import androidx.core.view.get
import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import kotlinx.coroutines.delay

// --- Wrapper View to intercept Action Mode (Selection Menu) ---
private class ReadiumWrapperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onAskAira: (String) -> Unit = {}
    var currentSelectedText: String? = null // Tracks the active highlight

    val container = FragmentContainerView(context).also { addView(it) }

    override fun startActionModeForChild(
        originalView: View,
        callback: ActionMode.Callback,
        type: Int
    ): ActionMode? {
        // Wrap the callback to customize the menu and dynamically fetch selected text
        val wrapped = if (callback is ActionMode.Callback2) {
            WrappedCallback2(callback, onAskAira) { currentSelectedText }
        } else {
            WrappedCallback(callback, onAskAira) { currentSelectedText }
        }
        return super.startActionModeForChild(originalView, wrapped, type)
    }
}

// --- Menu Customization ---
private class WrappedCallback(
    private val original: ActionMode.Callback,
    private val onAskAira: (String) -> Unit,
    private val getSelectedText: () -> String?
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode, menu: Menu) = original.onCreateActionMode(mode, menu)

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        original.onPrepareActionMode(mode, menu)
        // Remove unwanted items
        for (i in menu.size - 1 downTo 0) {
            val title = menu[i].title.toString()
            if (title.contains("read aloud", true) || title.contains("define", true)) {
                menu.removeItem(menu[i].itemId)
            }
        }
        // Add Custom "Ask Aira" Item
        if (menu.findItem(ASKAIRA) == null) {
            menu.add(Menu.NONE, ASKAIRA, Menu.NONE, "Ask Aira")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == ASKAIRA) {
            // Trigger Aira with the real text, falling back to a dummy string if missing
            onAskAira(getSelectedText() ?: "Processing selection...")
            mode.finish()
            return true
        }
        return original.onActionItemClicked(mode, item)
    }

    override fun onDestroyActionMode(mode: ActionMode) = original.onDestroyActionMode(mode)
}

private class WrappedCallback2(
    private val original: ActionMode.Callback2,
    onAskAira: (String) -> Unit,
    getSelectedText: () -> String?
) : ActionMode.Callback2() {
    private val delegate = WrappedCallback(original, onAskAira, getSelectedText)

    override fun onCreateActionMode(mode: ActionMode, menu: Menu) = delegate.onCreateActionMode(mode, menu)
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = delegate.onPrepareActionMode(mode, menu)
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = delegate.onActionItemClicked(mode, item)
    override fun onDestroyActionMode(mode: ActionMode) = delegate.onDestroyActionMode(mode)
    override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) = original.onGetContentRect(mode, view, outRect)
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReadiumFragmentHost(
    publication: Publication,
    fragmentTag: String,
    initialLocation: Locator?,
    preferences: EpubPreferences,
    onTap: () -> Unit,
    onAskAira: (selectedText: String) -> Unit = {},
    onNavigatorReady: (EpubNavigatorFragment) -> Unit
) {
    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnNavigatorReady by rememberUpdatedState(onNavigatorReady)
    val currentOnAskAira by rememberUpdatedState(onAskAira)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            ReadiumWrapperView(ctx).apply {
                container.id = View.generateViewId()
                clipToPadding = false
                fitsSystemWindows = false
                ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ -> WindowInsetsCompat.CONSUMED }

                // Wait for view to attach before manipulating Fragments
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        val activity = ctx as? FragmentActivity ?: return
                        attachNavigator(
                            activity,
                            this@apply, // Pass the wrapper view to track text
                            container.id,
                            fragmentTag,
                            publication,
                            initialLocation,
                            preferences,
                            currentOnTap,
                            currentOnNavigatorReady
                        )
                        removeOnAttachStateChangeListener(this)
                    }
                    override fun onViewDetachedFromWindow(v: View) = Unit
                })
            }
        },
        update = { view -> view.onAskAira = currentOnAskAira }
    )

    // Cleanup when Composable leaves the screen
    DisposableEffect(fragmentTag) {
        onDispose {
            val fm = (context as? FragmentActivity)?.supportFragmentManager ?: return@onDispose
            val fragment = fm.findFragmentByTag(fragmentTag)
            if (fragment != null && !fm.isStateSaved) {
                fm.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
            }
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun attachNavigator(
    activity: FragmentActivity,
    wrapperView: ReadiumWrapperView,
    containerId: Int,
    fragmentTag: String,
    publication: Publication,
    initialLocation: Locator?,
    preferences: EpubPreferences,
    onTap: () -> Unit,
    onNavigatorReady: (EpubNavigatorFragment) -> Unit
) {
    val fm = activity.supportFragmentManager

    // Check if it already exists (e.g. rotation)
    var fragment = fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment

    if (fragment == null) {
        val factory = EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocation,
            initialPreferences = preferences,
            configuration = buildNavigatorConfig()
        )

        // Use the custom factory to instantiate
        fm.fragmentFactory = factory
        fragment = fm.fragmentFactory.instantiate(
            activity.classLoader,
            EpubNavigatorFragment::class.java.name
        ) as EpubNavigatorFragment

        fm.commitNow { replace(containerId, fragment, fragmentTag) }
    }

    // Attach Input Listener
    fragment.addInputListener(object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            onTap()
            return true
        }
    })

    // Notify Parent
    onNavigatorReady(fragment)

    // Silently collect Readium's selection state and give it to our wrapper
    // In attachNavigator function
    // In attachNavigator function within ReadiumFragmentHost.kt
    activity.lifecycleScope.launch {
        // Since currentSelection() is a suspend function,
        // we poll for changes while the navigator is active.
        while (true) {
            val selection = fragment.currentSelection() // Now correctly called as a function
            wrapperView.currentSelectedText = selection?.locator?.text?.highlight

            // Small delay to prevent blocking the thread
            delay(200)
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
private fun buildNavigatorConfig() = EpubNavigatorFragment.Configuration().apply {
    servedAssets = servedAssets + "font/.*"
    shouldApplyInsetsPadding = false
    registerFonts(this)
}

@OptIn(ExperimentalReadiumApi::class)
private fun registerFonts(config: EpubNavigatorFragment.Configuration) {
    fun addFont(family: FontFamily, file: String) {
        config.addFontFamilyDeclaration(family) {
            addFontFace { addSource("font/$file", preload = true); setFontStyle(FontStyle.NORMAL) }
        }
    }

    // 1. Google Sans
    addFont(ReaderDefaults.FONT_GOOGLE_SANS, "google_sans_flex.ttf")

    // 2. Literata
    addFont(ReaderDefaults.FONT_LITERATA, "literata.ttf")

    // 3. Goudy Bookletter
    addFont(ReaderDefaults.FONT_GOUDY, "goudy.ttf")

    // 4. EB Garamond
    addFont(ReaderDefaults.FONT_GARAMOND, "eb_garamond.ttf")
}