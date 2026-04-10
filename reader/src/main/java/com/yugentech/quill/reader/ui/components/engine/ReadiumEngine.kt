package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    isPro: Boolean = false,
    initialLocation: Locator?,
    targetJumpHref: String?,
    targetSeekProgress: Double?,
    targetLocator: Locator? = null,
    allPositions: List<Locator>,
    preferences: EpubPreferences,
    isAiraReady: Boolean = false,
    onTap: () -> Unit,
    onAskAira: (String) -> Unit = {},
    onJumpComplete: () -> Unit,
    onSeekComplete: () -> Unit,
    onTargetLocatorComplete: () -> Unit = {},
    onLocatorChange: (Locator) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val fragmentTag = remember(bookId) { "readium_$bookId" }

    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val cssColorString = remember(selectionColor) { selectionColor.toCssRgba() }

    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ReadiumFragmentHost(
            publication = publication,
            fragmentTag = fragmentTag,
            initialLocation = initialLocation,
            preferences = preferences,
            isPro = isPro,
            onTap = onTap,
            onAskAira = onAskAira,
            onNavigatorReady = { nav ->
                navigator = nav
            }
        )
    }

    LaunchedEffect(targetLocator, navigator) {
        val nav = navigator ?: return@LaunchedEffect
        targetLocator?.let { loc ->

            val smoothScrollJs = """
                (function() {
                    document.documentElement.style.scrollBehavior = 'smooth';
                    document.body.style.scrollBehavior = 'smooth';
                })();
            """.trimIndent()

            try { nav.evaluateJavascript(smoothScrollJs) } catch (e: Exception) {}

            delay(50)

            nav.go(loc, animated = false)

            delay(400)

            val resetJs = """
                (function() {
                    document.documentElement.style.scrollBehavior = 'auto';
                    document.body.style.scrollBehavior = 'auto';
                })();
            """.trimIndent()

            try { nav.evaluateJavascript(resetJs) } catch (e: Exception) {}

            onTargetLocatorComplete()
        }
    }

    LaunchedEffect(preferences, navigator) {
        navigator?.submitPreferences(preferences)
    }

    LaunchedEffect(targetJumpHref, navigator) {
        val nav = navigator ?: return@LaunchedEffect
        targetJumpHref?.let { href ->
            Url(href)?.let { url ->
                publication.linkWithHref(url)?.let { link ->
                    publication.locatorFromLink(link)?.let { loc ->
                        nav.go(loc, animated = false)
                    }
                }
            }
            onJumpComplete()
        }
    }

    LaunchedEffect(targetSeekProgress, navigator) {
        val nav = navigator ?: return@LaunchedEffect
        targetSeekProgress?.let { progress ->
            if (allPositions.isNotEmpty()) {
                val index = (progress * (allPositions.size - 1)).roundToInt()
                    .coerceIn(0, allPositions.size - 1)
                nav.go(allPositions[index], animated = false)
            }
            onSeekComplete()
        }
    }

    LaunchedEffect(navigator, cssColorString) {
        val nav = navigator ?: return@LaunchedEffect

        suspend fun injectSelectionColor() {
            val js = """
                (function() {
                    var id = 'quill-selection-fix';
                    var existing = document.getElementById(id);
                    if (existing) existing.remove();
                    var s = document.createElement('style');
                    s.id = id;
                    s.textContent = '::selection { background-color: $cssColorString !important; color: inherit !important; }';
                    document.head.appendChild(s);
                })();
            """
            try {
                nav.evaluateJavascript(js)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        injectSelectionColor()

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            nav.currentLocator.collectLatest { locator ->
                injectSelectionColor()
                onLocatorChange(locator)
            }
        }
    }
}

private fun Color.toCssRgba(): String {
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return "rgba($r, $g, $b, $alpha)"
}