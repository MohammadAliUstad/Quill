package com.yugentech.quill.reader.reader.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import kotlin.math.roundToInt

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReadiumEngine(
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val fragmentTag = remember(bookId) { "readium_$bookId" }

    ReadiumFragmentHost(
        publication = publication,
        fragmentTag = fragmentTag,
        initialLocation = initialLocation,
        preferences = preferences,
        onTap = onTap
    )

    LaunchedEffect(preferences) {
        val fragment = (context as? FragmentActivity)?.supportFragmentManager
            ?.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
        fragment?.submitPreferences(preferences)
    }

    LaunchedEffect(targetJumpHref) {
        targetJumpHref?.let { href ->
            val fragment = (context as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment

            Url(href)?.let { url ->
                publication.linkWithHref(url)?.let { link ->
                    publication.locatorFromLink(link)?.let { locator ->
                        fragment?.go(locator, animated = false)
                    }
                }
            }
            onJumpComplete()
        }
    }

    LaunchedEffect(targetSeekProgress) {
        targetSeekProgress?.let { progress ->
            val fragment = (context as? FragmentActivity)?.supportFragmentManager
                ?.findFragmentByTag(fragmentTag) as? EpubNavigatorFragment

            if (allPositions.isNotEmpty() && fragment != null) {
                val targetIndex = (progress * (allPositions.size - 1))
                    .roundToInt()
                    .coerceIn(0, allPositions.size - 1)
                fragment.go(allPositions[targetIndex], animated = false)
            }
            onSeekComplete()
        }
    }

    LaunchedEffect(bookId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val activity = context as? FragmentActivity ?: return@repeatOnLifecycle
            var fragment: EpubNavigatorFragment? = null

            while (fragment == null) {
                fragment = activity.supportFragmentManager
                    .findFragmentByTag(fragmentTag) as? EpubNavigatorFragment
                if (fragment == null) delay(100)
            }

            fragment.currentLocator.collectLatest { onLocatorChange(it) }
        }
    }
}