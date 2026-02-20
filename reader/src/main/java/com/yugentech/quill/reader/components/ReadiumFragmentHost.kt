package com.yugentech.quill.reader.reader.components

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

private const val ASK_AIRA_ITEM_ID = 1001

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReadiumFragmentHost(
    publication: Publication,
    fragmentTag: String,
    initialLocation: Locator?,
    preferences: EpubPreferences,
    onTap: () -> Unit,
    onAskAira: (selectedText: String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnAskAira by rememberUpdatedState(onAskAira)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { factoryContext ->

            val interceptLayout = object : FrameLayout(factoryContext) {
                override fun startActionModeForChild(
                    originalView: View,
                    callback: ActionMode.Callback,
                    type: Int
                ): ActionMode {
                    val callback2 = callback as? ActionMode.Callback2

                    val wrapper = object : ActionMode.Callback2() {
                        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean =
                            callback.onCreateActionMode(mode, menu)

                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                            val chromeWantsUpdate = callback.onPrepareActionMode(mode, menu)
                            var changed = false

                            for (i in (menu.size - 1) downTo 0) {
                                val item = menu[i]
                                if (item.title?.contains("Read Aloud", ignoreCase = true) == true) {
                                    menu.removeItem(item.itemId)
                                    changed = true
                                }
                            }

                            if (menu.findItem(ASK_AIRA_ITEM_ID) == null) {
                                menu.add(Menu.NONE, ASK_AIRA_ITEM_ID, Menu.FIRST, "Ask Aira")
                                changed = true
                            }

                            return chromeWantsUpdate || changed
                        }

                        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
                            if (item.itemId == ASK_AIRA_ITEM_ID) {
                                currentOnAskAira("")
                                mode.finish()
                                true
                            } else {
                                callback.onActionItemClicked(mode, item)
                            }

                        override fun onDestroyActionMode(mode: ActionMode) =
                            callback.onDestroyActionMode(mode)

                        // Inside your startActionModeForChild -> wrapper -> onGetContentRect
                        override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
                            if (callback2 != null) {
                                callback2.onGetContentRect(mode, view, outRect)
                            } else {
                                super.onGetContentRect(mode, view, outRect)
                            }

                            // ── THE AUTOSCROLL TOOL ──
                            // Detect if the user is dragging handles near the bottom or top of the screen
                            val scrollThreshold = 100 // pixels from edge
                            val activity = factoryContext as? FragmentActivity
                            val fragment = activity?.supportFragmentManager?.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment

                            if (outRect.bottom > view.height - scrollThreshold) {
                                // Handle is at the bottom: Nudge the navigator forward
                                fragment?.goForward(animated = true)
                            } else if (outRect.top < scrollThreshold) {
                                // Handle is at the top: Nudge the navigator backward
                                fragment?.goBackward(animated = true)
                            }

                            // Keep your flickering fixes, but don't clamp so hard that handles can't "hit" the edge
                            outRect.left = outRect.left.coerceAtLeast(0)
                            outRect.right = outRect.right.coerceIn(outRect.left, view.width)

                            if (outRect.top < 250) {
                                outRect.top = 0
                            }
                        }
                    }

                    return super.startActionModeForChild(originalView, wrapper, type)
                }
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                fitsSystemWindows = false
                clipChildren = false
            }

            val container = FragmentContainerView(factoryContext).apply {
                id = View.generateViewId()
                clipToPadding = false
                fitsSystemWindows = false

                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        val activity = factoryContext as? FragmentActivity ?: return
                        val fragmentManager = activity.supportFragmentManager

                        if (fragmentManager.findFragmentByTag(fragmentTag) is EpubNavigatorFragment) {
                            removeOnAttachStateChangeListener(this)
                            return
                        }

                        val fragmentConfiguration = EpubNavigatorFragment.Configuration().apply {
                            servedAssets = servedAssets + "fonts/.*"

                            addFontFamilyDeclaration(ReaderDefaults.FONT_GOOGLE_SANS) {
                                addFontFace {
                                    addSource("fonts/google_sans_flex.ttf", preload = true)
                                    setFontStyle(FontStyle.NORMAL)
                                }
                            }
                            addFontFamilyDeclaration(ReaderDefaults.FONT_LITERATA) {
                                addFontFace {
                                    addSource("fonts/literata.ttf", preload = true)
                                    setFontStyle(FontStyle.NORMAL)
                                }
                            }
                            addFontFamilyDeclaration(ReaderDefaults.FONT_GOUDY) {
                                addFontFace {
                                    addSource("fonts/goudy.ttf", preload = true)
                                    setFontStyle(FontStyle.NORMAL)
                                }
                            }
                            addFontFamilyDeclaration(ReaderDefaults.FONT_GARAMOND) {
                                addFontFace {
                                    addSource("fonts/eb_garamond.ttf", preload = true)
                                    setFontStyle(FontStyle.NORMAL)
                                }
                            }
                        }

                        val fragmentFactory = EpubNavigatorFactory(publication)
                            .createFragmentFactory(
                                initialLocator = initialLocation,
                                initialPreferences = preferences,
                                configuration = fragmentConfiguration
                            )

                        fragmentManager.fragmentFactory = fragmentFactory

                        val fragment = fragmentManager.fragmentFactory.instantiate(
                            factoryContext.classLoader,
                            EpubNavigatorFragment::class.java.name
                        ) as EpubNavigatorFragment

                        fragmentManager.commitNow {
                            replace(id, fragment, fragmentTag)
                        }

                        fragment.addInputListener(object : InputListener {
                            override fun onTap(event: TapEvent): Boolean {
                                currentOnTap()
                                return true
                            }
                        })

                        removeOnAttachStateChangeListener(this)
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                })
            }

            interceptLayout.addView(container)
            interceptLayout
        },
        update = {}
    )

    DisposableEffect(fragmentTag) {
        onDispose {
            val activity = context as? FragmentActivity ?: return@onDispose
            val fragmentManager = activity.supportFragmentManager
            val fragment = fragmentManager.findFragmentByTag(fragmentTag) ?: return@onDispose
            if (!fragmentManager.isStateSaved) {
                fragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNowAllowingStateLoss()
            }
        }
    }
}