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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import com.yugentech.quill.reader.viewmodel.ReaderCommand
import kotlin.math.roundToInt

private fun Color.toCssRgba(): String {
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return "rgba($r, $g, $b, $alpha)"
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReadiumEngine(
    modifier: Modifier = Modifier,
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
    decorations: List<Decoration> = emptyList(),
    commands: Flow<ReaderCommand>? = null,
    onTap: () -> Unit,
    onAskAira: (String) -> Unit = {},
    onSelectionAction: (Locator) -> Unit = {},
    onDecorationTapped: (Decoration) -> Unit = {},
    onJumpComplete: () -> Unit,
    onSeekComplete: () -> Unit,
    onTargetLocatorComplete: () -> Unit = {},
    onLocatorChange: (Locator) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // FIX: Include scroll mode in tag to allow concurrent fragments during crossfade
    val fragmentTag = remember(bookId, preferences.scroll) {
        "readium_${bookId}_${if (preferences.scroll == true) "scroll" else "paged"}"
    }

    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val cssColorString = remember(selectionColor) { selectionColor.toCssRgba() }

    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        ReadiumFragmentHost(
            publication = publication,
            fragmentTag = fragmentTag,
            initialLocation = initialLocation,
            preferences = preferences,
            isPro = isPro,
            isAiraReady = isAiraReady,
            onTap = onTap,
            onAskAira = onAskAira,
            onHighlightRequest = { locator ->
                onSelectionAction(locator)
            },
            onNavigatorReady = { nav ->
                navigator = nav

                (nav as? DecorableNavigator)?.addDecorationListener(
                    "user_highlights",
                    object : DecorableNavigator.Listener {
                        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                            onDecorationTapped(event.decoration)
                            return true
                        }
                    }
                )
            }
        )
    }

    LaunchedEffect(decorations, navigator) {
        val decorableNav = navigator as? DecorableNavigator
        decorableNav?.applyDecorations(decorations, "user_highlights")
    }

    LaunchedEffect(commands, navigator) {
        val nav = navigator ?: return@LaunchedEffect
        commands?.collect { command ->
            when (command) {
                ReaderCommand.NextPage -> nav.goForward(animated = true)
                ReaderCommand.PreviousPage -> nav.goBackward(animated = true)
            }
        }
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

        suspend fun injectSelectionStyles() {
            val js = """
                (function() {
                    var id = 'quill-selection-style';
                    var existing = document.getElementById(id);
                    if (existing) existing.remove();
                    var s = document.createElement('style');
                    s.id = id;
                    s.textContent = `
                        ::selection {
                            background-color: $cssColorString !important;
                            color: inherit !important;
                        }
                    `;
                    document.head.appendChild(s);
                })();
            """.trimIndent()

            try {
                nav.evaluateJavascript(js)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun injectSelectionHandler() {
            val js = """
                (function() {
                    if (window.__quillSelHandlerInstalled) return;
                    window.__quillSelHandlerInstalled = true;

                    var _timer = null;
                    var _listening = true;

                    function _firstVisible() {
                        var w = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
                        var n;
                        while ((n = w.nextNode())) {
                            if (!n.textContent.trim()) continue;
                            var r = document.createRange();
                            r.selectNodeContents(n);
                            var rect = r.getBoundingClientRect();
                            if (rect.left >= 0 && rect.left < window.innerWidth && rect.right > 0) return n;
                        }
                        return null;
                    }

                    function _doClamp() {
                        if (!_listening) return;
                        var sel = window.getSelection();
                        if (!sel || sel.rangeCount === 0 || sel.isCollapsed) return;
                        var range = sel.getRangeAt(0);
                        var anc = range.startContainer;
                        var probe = document.createRange();
                        if (anc.nodeType === 3 && anc.length > 0) {
                            var off = Math.min(range.startOffset, anc.length - 1);
                            probe.setStart(anc, off); probe.setEnd(anc, off + 1);
                        } else {
                            probe.setStart(anc, range.startOffset); probe.collapse(true);
                        }
                        if (probe.getBoundingClientRect().left >= 0) return;
                        var fn = _firstVisible();
                        if (!fn) return;
                        _listening = false;
                        try { sel.setBaseAndExtent(fn, 0, range.endContainer, range.endOffset); } catch (e) {}
                        setTimeout(function() { _listening = true; }, 300);
                    }

                    // Debounced: only clamp 150ms after the last selectionchange.
                    // Clamping on every event causes a tug-of-war with native selection → flicker.
                    document.addEventListener('selectionchange', function() {
                        if (!_listening) return;
                        clearTimeout(_timer);
                        _timer = setTimeout(_doClamp, 150);
                    });
                })();
            """.trimIndent()

            try {
                nav.evaluateJavascript(js)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        injectSelectionStyles()
        injectSelectionHandler()

        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            nav.currentLocator.collectLatest { locator ->
                injectSelectionStyles()
                injectSelectionHandler()
                onLocatorChange(locator)
            }
        }
    }
}