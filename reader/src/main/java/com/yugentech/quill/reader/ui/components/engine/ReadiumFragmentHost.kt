package com.yugentech.quill.reader.ui.components.engine

import android.view.View
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
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReadiumFragmentHost(
    publication: Publication,
    fragmentTag: String,
    initialLocation: Locator?,
    preferences: EpubPreferences,
    onTap: () -> Unit,
    isPro: Boolean = false,
    isAiraReady: Boolean = false,
    onAskAira: (selectedText: String) -> Unit = {},
    onHighlightRequest: (Locator) -> Unit = {},
    onSelectionStarted: () -> Unit = {},
    onSelectionEnded: () -> Unit = {},
    onNavigatorReady: (EpubNavigatorFragment) -> Unit
) {

    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnNavigatorReady by rememberUpdatedState(onNavigatorReady)
    val currentOnAskAira by rememberUpdatedState(onAskAira)
    val currentOnHighlightRequest by rememberUpdatedState(onHighlightRequest)
    val currentOnSelectionStarted by rememberUpdatedState(onSelectionStarted)
    val currentOnSelectionEnded by rememberUpdatedState(onSelectionEnded)
    val currentIsPro by rememberUpdatedState(isPro)
    val currentIsAiraReady by rememberUpdatedState(isAiraReady)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            ReadiumWrapperView(ctx).apply {
                container.id = View.generateViewId()
                clipToPadding = false
                fitsSystemWindows = false
                this.isPro = currentIsPro
                this.isAiraReady = currentIsAiraReady

                ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ -> WindowInsetsCompat.CONSUMED }

                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        val activity = ctx as? FragmentActivity ?: return
                        attachNavigator(
                            activity,
                            this@apply,
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
        update = { view ->
            view.isPro = currentIsPro
            view.isAiraReady = currentIsAiraReady
            view.onAskAira = currentOnAskAira
            view.onHighlightRequest = currentOnHighlightRequest
            view.onSelectionStarted = currentOnSelectionStarted
            view.onSelectionEnded = currentOnSelectionEnded
        }
    )

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