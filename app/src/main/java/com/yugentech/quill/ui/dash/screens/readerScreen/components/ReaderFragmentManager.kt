package com.yugentech.quill.ui.dash.screens.readerScreen.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderFragmentManager(
    publication: Publication,
    bookId: String,
    initialLocation: Locator?,
    targetJumpHref: String?,
    targetSeekProgress: Double?,
    allPositions: List<Locator>,
    preferences: EpubPreferences,
    onTap: () -> Unit,
    onJumpComplete: () -> Unit,
    onSeekComplete: () -> Unit,
    onLocatorChange: (Locator) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val fragmentTag = remember(bookId) { "readium_$bookId" }

    // Preference updates
    LaunchedEffect(preferences) {
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        val fragment = activity.supportFragmentManager
            .findFragmentByTag(fragmentTag) as? EpubNavigatorFragment

        fragment?.submitPreferences(preferences)
    }

    // Handle chapter jumps
    LaunchedEffect(targetJumpHref) {
        targetJumpHref ?: return@LaunchedEffect

        val activity = context as? FragmentActivity ?: run {
            onJumpComplete()
            return@LaunchedEffect
        }

        val fragment = activity.supportFragmentManager
            .findFragmentByTag(fragmentTag) as? EpubNavigatorFragment ?: run {
            Timber.w("Fragment not ready for navigation")
            onJumpComplete()
            return@LaunchedEffect
        }

        Url(targetJumpHref)?.let { url ->
            publication.linkWithHref(url)?.let { link ->
                publication.locatorFromLink(link)?.let { locator ->
                    fragment.go(locator, animated = false)
                }
            }
        }

        onJumpComplete()
    }

    // Handle seekbar
    LaunchedEffect(targetSeekProgress) {
        targetSeekProgress ?: return@LaunchedEffect

        val activity = context as? FragmentActivity ?: run {
            onSeekComplete()
            return@LaunchedEffect
        }

        val fragment = activity.supportFragmentManager
            .findFragmentByTag(fragmentTag) as? EpubNavigatorFragment ?: run {
            Timber.w("Fragment not ready for seek")
            onSeekComplete()
            return@LaunchedEffect
        }

        if (allPositions.isNotEmpty()) {
            val targetIndex = (targetSeekProgress * (allPositions.size - 1))
                .roundToInt()
                .coerceIn(0, allPositions.size - 1)

            fragment.go(allPositions[targetIndex], animated = false)
        }

        onSeekComplete()
    }

    // Lifecycle-aware locator observation
    LaunchedEffect(bookId) {
        val activity = context as? FragmentActivity ?: return@LaunchedEffect

        // Note: This logic assumes the fragment is already attached.
        // In a real scenario, you might need to retry if it's null initially.
        // But the AndroidView below handles the creation.

        // Use repeatOnLifecycle
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // We loop until we find the fragment to attach the collector
            var fragment: EpubNavigatorFragment? = null
            while (fragment == null) {
                fragment = activity.supportFragmentManager.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
                if (fragment == null) kotlinx.coroutines.delay(100)
            }

            fragment.currentLocator.collectLatest { locator ->
                onLocatorChange(locator)
            }
        }
    }

    ReadiumFragmentHost(
        publication = publication,
        fragmentTag = fragmentTag,
        initialLocation = initialLocation,
        preferences = preferences,
        onTap = onTap
    )
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
private fun ReadiumFragmentHost(
    publication: Publication,
    fragmentTag: String,
    initialLocation: Locator?,
    preferences: EpubPreferences,
    onTap: () -> Unit
) {
    val context = LocalContext.current

    val inputListener = remember {
        object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                onTap()
                return true
            }
        }
    }

    AndroidView(
        factory = { factoryContext ->
            val containerId = View.generateViewId()

            FragmentContainerView(factoryContext).apply {
                id = containerId

                // --- FIX STARTS HERE ---
                // We Wait for the view to be attached to the window before committing the fragment.
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        val activity = factoryContext as? FragmentActivity ?: return
                        val fragmentManager = activity.supportFragmentManager

                        // If a fragment already exists (e.g. from rotation), we replace it to ensure
                        // it is attached to THIS new container ID.
                        // If we don't do this, the FragmentManager might keep it attached to a "dead" previous view ID.
                        val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)

                        // Setup Factory
                        val navigatorFactory = EpubNavigatorFactory(publication)
                        val fragmentFactory = navigatorFactory.createFragmentFactory(
                            initialLocator = initialLocation,
                            initialPreferences = preferences
                        )
                        fragmentManager.fragmentFactory = fragmentFactory

                        // Instantiate
                        val fragment = existingFragment as? EpubNavigatorFragment
                            ?: fragmentManager.fragmentFactory.instantiate(
                                factoryContext.classLoader,
                                EpubNavigatorFragment::class.java.name
                            ) as EpubNavigatorFragment

                        // Commit Transaction
                        // We use 'replace' to handle both creation and "moving" existing fragments to the new container
                        fragmentManager.commitNow {
                            replace(containerId, fragment, fragmentTag)
                        }

                        // Add listener
                        fragment.addInputListener(inputListener)

                        // Cleanup listener
                        removeOnAttachStateChangeListener(this)
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                })
                // --- FIX ENDS HERE ---
            }
        },
        update = { /* Stateless */ }
    )

    DisposableEffect(fragmentTag) {
        onDispose {
            val activity = context as? FragmentActivity ?: return@onDispose
            val fragmentManager = activity.supportFragmentManager
            val fragment = fragmentManager.findFragmentByTag(fragmentTag)

            fragment?.let {
                if (!fragmentManager.isStateSaved) {
                    fragmentManager.beginTransaction()
                        .remove(it)
                        .commitNowAllowingStateLoss()
                }
            }
        }
    }
}