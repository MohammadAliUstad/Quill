package com.yugentech.quill.ui.dash.screens.readerScreen.components

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Url

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReadiumFragmentHost(
    publication: Publication,
    fragmentTag: String,
    initialChapterHref: String?,
    preferences: EpubPreferences,
    onTap: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(fragmentTag) {
        onDispose {
            val fragmentActivity = context as? FragmentActivity
            val fm = fragmentActivity?.supportFragmentManager
            val fragment = fm?.findFragmentByTag(fragmentTag)

            if (fragment != null) {
                fm.beginTransaction().remove(fragment).commitAllowingStateLoss()
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = View.generateViewId()
                tag = fragmentTag
            }
        },
        update = { view ->
            val fragmentActivity = context as? FragmentActivity ?: return@AndroidView
            val fm = fragmentActivity.supportFragmentManager

            if (fm.findFragmentByTag(fragmentTag) == null) {
                val navigatorFactory = EpubNavigatorFactory(publication)
                val initialLocator = initialChapterHref?.let { href ->
                    Url(href)?.let { url ->
                        publication.linkWithHref(url)?.let { link ->
                            publication.locatorFromLink(link)
                        }
                    }
                }

                val standardListener = object : EpubNavigatorFragment.Listener {
                    override fun onExternalLinkActivated(url: AbsoluteUrl) { /* Handle external */
                    }
                }

                val config = navigatorFactory.createFragmentFactory(
                    initialLocator = initialLocator,
                    listener = standardListener
                )

                fm.fragmentFactory = config
                fm.commitNow {
                    replace(view.id, EpubNavigatorFragment::class.java, Bundle(), fragmentTag)
                }
            }

            val fragment = fm.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
            fragment?.let { navigator ->
                navigator.submitPreferences(preferences)

                navigator.addInputListener(object : InputListener {
                    override fun onTap(event: TapEvent): Boolean {
                        val screenWidth = view.width
                        val x = event.point.x

                        val leftBoundary = screenWidth * 0.2
                        val rightBoundary = screenWidth * 0.8

                        if (x > leftBoundary && x < rightBoundary) {
                            onTap()
                            return true
                        }
                        return false
                    }
                })
            }
        }
    )
}