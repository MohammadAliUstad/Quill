package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yugentech.quill.reader.ui.components.highlightSheet.luminance
import com.yugentech.quill.reader.viewmodel.reader.ReaderCommand
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import kotlin.math.roundToInt

private fun Color.toCssRgba(): String {
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return "rgba($r, $g, $b, $alpha)"
}

/**
 * How many selection changes in one burst mean a handle is being dragged (the toolbar hides).
 * Fewer is the system adjusting a fresh selection, and the toolbar stays put through it.
 */
private const val DRAG_CHANGE_COUNT = 3

/** Least room kept between the toolbar and the screen's top or bottom edge, in dp. */
private const val SCREEN_EDGE_MARGIN = 8f

// SelectionToolbar's real height: 9dp padding above and below, each action 4dp + a 32dp icon
// slot + 4dp + a 16sp label line + 4dp. At 66 a toolbar placed above the selection overlapped
// its first line.
private const val TOOLBAR_HEIGHT_DP = 78f

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
    onAskAira: (String, Locator?) -> Unit = { _, _ -> },
    onSelectionAction: (Locator) -> Unit = {},
    onDecorationTapped: (Decoration) -> Unit = {},
    onJumpComplete: () -> Unit,
    onSeekComplete: () -> Unit,
    onTargetLocatorComplete: () -> Unit = {},
    onLocatorChange: (Locator) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fragmentTag = remember(bookId, preferences.scroll) {
        "readium_${bookId}_${if (preferences.scroll == true) "scroll" else "paged"}"
    }

    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val cssColorString = remember(selectionColor) { selectionColor.toCssRgba() }

    // True when the ebook background is light (white/sepia/cream) so the toolbar can
    // switch to a more opaque style that stays visible on pale page backgrounds.
    val readerBgIsLight = remember(preferences.backgroundColor) {
        (preferences.backgroundColor?.let { Color(it.int) } ?: Color.White).luminance() > 0.5f
    }

    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var selectionInfo by remember { mutableStateOf<SelectionInfo?>(null) }
    var lastHapticText by remember { mutableStateOf<String?>(null) }
    var lastBridgeText by remember { mutableStateOf("") }
    var isClearingSelection by remember { mutableStateOf(false) }
    var clearNativeSelectionRequest by remember { mutableStateOf<(() -> Unit)?>(null) }
    var toolbarY by remember { mutableStateOf(0.dp) }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val scope = rememberCoroutineScope()

    suspend fun clearSelection() {
        val nav = navigator ?: return
        if (isClearingSelection) return
        isClearingSelection = true
        
        selectionInfo = null
        lastBridgeText = ""
        
        try {
            // 1. Force-finish the native Android ActionMode to hide handles instantly.
            clearNativeSelectionRequest?.invoke()

            // 2. Use the navigator's built-in clearSelection and augment with JS to ensure
            // the bridge data and native selection are fully purged.
            nav.clearSelection()
            val clearJs = """
                (function() {
                    window.__quillSelData = null;
                    window.__quillSelChanging = false;
                    var sel = window.getSelection();
                    if (sel) {
                        if (sel.empty) sel.empty();
                        if (sel.removeAllRanges) sel.removeAllRanges();
                    }
                    if (document.activeElement && document.activeElement.blur) {
                        document.activeElement.blur();
                    }
                })()
            """.trimIndent()
            nav.evaluateJavascript(clearJs)
            // A brief delay then a second clear attempt handles cases where the
            // first call was ignored due to an ongoing handle animation.
            delay(100)
            nav.evaluateJavascript(clearJs)
        } catch (_: Exception) {}
        
        // Keep the flag active for a brief cooldown to ensure the polling loop 
        // doesn't catch stale data during the WebView's asynchronous clear process.
        delay(150)
        isClearingSelection = false
    }

    suspend fun syncSelection(jsonStr: String?) {
        val nav = navigator ?: return
        if (jsonStr == null || jsonStr == "null" || jsonStr == "undefined") {
            selectionInfo = null
            return
        }

        try {
            val obj = JSONObject(jsonStr)
            val text = obj.optString("text")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
            val rects = obj.optJSONArray("rects")
            val locator = nav.currentSelection()?.locator

            if (rects != null && rects.length() > 0 && text.isNotEmpty()) {
                val first = rects.getJSONObject(0)
                val last = rects.getJSONObject(rects.length() - 1)
                
                // Haptic feedback removed here to avoid overlap with the bridge's 
                // zero-lag feedback. lastHapticText updated for consistency.
                lastHapticText = text

                selectionInfo = SelectionInfo(
                    text = text,
                    locator = locator,
                    rectTop = first.getDouble("top").toFloat(),
                    rectBottom = last.getDouble("bottom").toFloat()
                )
                toolbarY = computeToolbarY(selectionInfo!!.rectTop, selectionInfo!!.rectBottom, screenHeightDp)
            } else {
                selectionInfo = null
            }
        } catch (e: Exception) {
            selectionInfo = null
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    Box(modifier = modifier.fillMaxSize()) {
        ReadiumFragmentHost(
            publication = publication,
            fragmentTag = fragmentTag,
            initialLocation = initialLocation,
            preferences = preferences,
            isPro = isPro,
            isAiraReady = isAiraReady,
            onAskAira = { text, locator ->
                onAskAira(text, locator)
                scope.launch { clearSelection() }
            },
            onHighlightRequest = { locator ->
                onSelectionAction(locator)
                scope.launch { clearSelection() }
            },
            onSelectionStarted = {
                // Haptics removed to avoid double-triggering with onSelectionChanged
            },
            onSelectionEnded = {
                lastHapticText = null
                lastBridgeText = ""
            },
            onSelectionChanged = { text ->
                if (text.isNotBlank() && text != lastBridgeText) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastBridgeText = text
                }
            },
            onClearSelection = { request ->
                clearNativeSelectionRequest = request
            },
            onTap = {
                if (selectionInfo != null) {
                    scope.launch { clearSelection() }
                } else {
                    onTap()
                }
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
            },
            modifier = Modifier.hazeSource(hazeState)
        )

        val currentSelection = selectionInfo

        // Keep last non-null selection/position so the exit animation has content to render.
        var exitSelection by remember { mutableStateOf<SelectionInfo?>(null) }
        var exitToolbarY by remember { mutableStateOf(0.dp) }
        if (currentSelection != null) {
            exitSelection = currentSelection
            exitToolbarY = toolbarY
        }

        // A toolbar that's already up glides to its new place when the selection settles
        // somewhere else (the system widening it, say); one that's just appearing starts
        // right where it belongs instead of sliding over from the last selection's spot.
        val glideY = remember { Animatable(0f) }
        var gliding by remember { mutableStateOf(false) }
        LaunchedEffect(currentSelection != null, exitToolbarY) {
            if (currentSelection == null) {
                gliding = false
            } else if (!gliding) {
                glideY.snapTo(exitToolbarY.value)
                gliding = true
            } else {
                glideY.animateTo(exitToolbarY.value, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }

        AnimatedVisibility(
            visible = currentSelection != null,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.88f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.88f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    val y = if (gliding) glideY.value.dp else exitToolbarY
                    IntOffset(0, y.roundToPx())
                }
        ) {
            exitSelection?.let { sel ->
                // The selection is still live when an action is tapped, so ask Readium for its
                // locator then: the one captured when the toolbar appeared can be missing (a
                // selection starting in a paragraph carried over from the previous page), which
                // made Highlight do nothing at all.
                suspend fun freshLocator(): Locator? =
                    runCatching { navigator?.currentSelection()?.locator }.getOrNull() ?: sel.locator

                SelectionToolbar(
                    selectionInfo = sel,
                    isAiraReady = isAiraReady,
                    hazeState = hazeState,
                    readerBgIsLight = readerBgIsLight,
                    onHighlight = {
                        scope.launch {
                            freshLocator()?.let { onSelectionAction(it) }
                            clearSelection()
                        }
                    },
                    onAskAira = { text ->
                        scope.launch {
                            onAskAira(text, freshLocator())
                            clearSelection()
                        }
                    },
                    onCopy = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        scope.launch { clearSelection() }
                    },
                    onShare = { text ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, null))
                        scope.launch { clearSelection() }
                    }
                )
            }
        }
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

            val resetJs = """
                (function() {
                    document.documentElement.style.scrollBehavior = 'auto';
                    document.body.style.scrollBehavior = 'auto';
                })();
            """.trimIndent()

            try { nav.evaluateJavascript(smoothScrollJs) } catch (e: Exception) {}

            try {
                delay(50)
                nav.go(loc, animated = false)
                delay(400)
            } finally {
                // Always undo smooth scrolling, even when this jump is interrupted: left on, it
                // turns every later scroll into a slide (Chromium's selection reveal included,
                // which then flickers past the selection guard)
                withContext(NonCancellable) {
                    try { nav.evaluateJavascript(resetJs) } catch (e: Exception) {}
                }
            }

            onTargetLocatorComplete()
        }
    }

    LaunchedEffect(preferences, navigator) {
        val nav = navigator ?: return@LaunchedEffect
        // Inject the new background colour directly into the WebView before Readium's
        // own preference update reaches the renderer. This closes the timing gap that
        // causes a "two-piece" flicker in paged mode: the Compose outer Box repaints
        // instantly while the WebView normally waits for the Readium CSS pipeline.
        preferences.backgroundColor?.let { readiumColor ->
            try {
                val cssColor = Color(readiumColor.int).toCssRgba()
                nav.evaluateJavascript(
                    "(function(){var c='$cssColor';var e=document.documentElement,b=document.body;" +
                        "if(e)e.style.backgroundColor=c;if(b)b.style.backgroundColor=c;})();"
                )
            } catch (_: Exception) {}
        }
        nav.submitPreferences(preferences)
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

    // Polling Loop for Selection Info (Transparent native selection tracking)
    LaunchedEffect(navigator) {
        val nav = navigator ?: return@LaunchedEffect
        // The payload the toolbar was last built from, so it's only rebuilt when that changes
        var lastSyncedJson: String? = null

        while (true) {
            // Checked often, so a settled selection gets its toolbar without a noticeable wait
            delay(50)
            val json = try {
                nav.evaluateJavascript("""
                    (function() {
                        // The selection is still changing. A run of changes is a handle being
                        // dragged: the toolbar steps aside. One or two is the system adjusting a
                        // fresh selection (Smart Text Selection widening it to a phrase): the
                        // toolbar holds still instead of vanishing and coming straight back.
                        if (window.__quillSelChanging) {
                            return (window.__quillSelChangeCount || 0) >= $DRAG_CHANGE_COUNT ? 'dragging' : 'settling';
                        }

                        var sel = window.getSelection();
                        var liveActive = sel && !sel.isCollapsed && sel.rangeCount > 0 && sel.toString();
                        if (liveActive) {
                            var range = sel.getRangeAt(0);
                            var rects = range.getClientRects();
                            var filteredRects = [];
                            var pageTol = window.innerWidth * 0.1;
                            for (var i = 0; i < rects.length; i++) {
                                var r = rects[i];
                                if (r.left >= -pageTol && r.left < window.innerWidth + pageTol) {
                                    filteredRects.push({top: r.top, bottom: r.bottom, left: r.left, right: r.right});
                                }
                            }
                            // Fallback: focus is always on the current page
                            if (filteredRects.length === 0 && sel.focusNode) {
                                try {
                                    var fr = document.createRange();
                                    var fo = Math.min(sel.focusOffset, sel.focusNode.nodeType === 3 ? sel.focusNode.length : sel.focusNode.childNodes.length);
                                    fr.setStart(sel.focusNode, fo);
                                    fr.collapse(true);
                                    var fRect = fr.getBoundingClientRect();
                                    if (fRect.height > 0) {
                                        filteredRects.push({top: fRect.top, bottom: fRect.bottom, left: fRect.left, right: fRect.right});
                                    }
                                } catch(e) {}
                            }
                            if (filteredRects.length > 0) {
                                var liveData = JSON.stringify({ text: sel.toString(), rects: filteredRects });
                                window.__quillSelData = liveData;
                                return liveData;
                            }
                        }
                        // If live selection is gone or produced no rects, use stored capture from
                        // selectionchange — handles the case where Readium collapsed the selection
                        // before the poll ran (cross-page anchor on a continuation paragraph).
                        var captured = window.__quillSelData;
                        return (typeof captured === 'string') ? captured : JSON.stringify(captured) || null;
                    })()
                """.trimIndent())
            } catch (_: Exception) { null }

            // evaluateJavascript hands back the script's result JSON-encoded, so the payload
            // (itself JSON) arrives as a JSON string literal. Decode it properly: stripping the
            // outer quotes and un-escaping \" by hand broke on any selection holding a straight
            // quote (the toolbar never appeared) and left \n and \\ escaped in copied text.
            val processedJson = json?.let { raw ->
                runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
            }

            if (isClearingSelection) {
                // Ignore poll results while a manual clear is in progress
                continue
            }

            when {
                // A handle is being dragged: hide until it's let go
                processedJson == "dragging" -> {
                    selectionInfo = null
                    lastSyncedJson = null
                }
                // A brief adjustment: keep whatever is showing (or not yet showing) as it is
                processedJson == "settling" -> Unit
                processedJson == null || processedJson == "null" || processedJson == "undefined" || processedJson == "{}" -> {
                    selectionInfo = null
                    lastSyncedJson = null
                }
                // A new or changed stable selection: show the toolbar, or bring the one showing
                // up to date (its text and place) when the selection settled differently.
                processedJson != lastSyncedJson -> {
                    syncSelection(processedJson)
                    lastSyncedJson = if (selectionInfo != null) processedJson else null
                }
            }
        }
    }

    LaunchedEffect(navigator, cssColorString) {
        val nav = navigator ?: return@LaunchedEffect

        suspend fun injectSelectionStyles() {
            // Restore the native selection highlight for zero-lag visual feedback.
            // We use the CSS ::selection selector to apply our brand color snappy.
            val bgColor = cssColorString
            val js = """
                (function() {
                    var id = 'quill-selection-style';
                    var existing = document.getElementById(id);
                    if (existing) {
                        existing.textContent = '::selection { background-color: $bgColor !important; }';
                        return;
                    }
                    var s = document.createElement('style');
                    s.id = id;
                    s.textContent = '::selection { background-color: $bgColor !important; }';
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
                    var scrollEl = document.scrollingElement || document.documentElement;

                    // Clear stored selection data on every page navigation so stale data
                    // from the previous page never leaks into the new page's toolbar.
                    window.__quillSelData = null;

                    // Always refresh page-start fallback on every page render/navigation.
                    // Prefer a node whose offset-0 is on the current page (not a continuation from the
                    // previous column), because Readium resolves the locator from the node start.
                    // If all visible text is a continuation, fall back to the caret position.
                    (function() {
                        var tol = window.innerWidth * 0.1;
                        function nodeStartOnPage(node) {
                            if (!node || node.nodeType !== 3) return false;
                            try {
                                var r = document.createRange();
                                r.setStart(node, 0); r.collapse(true);
                                var rect = r.getBoundingClientRect();
                                return rect.left > -tol && rect.left < window.innerWidth + tol;
                            } catch(e) { return false; }
                        }
                        var freshNode = null, fallbackNode = null;
                        for (var ty = 4; ty < window.innerHeight && !freshNode; ty += 24) {
                            var r0 = document.caretRangeFromPoint(4, ty);
                            if (!r0) continue;
                            if (!fallbackNode) {
                                fallbackNode = { node: r0.startContainer, offset: r0.startOffset };
                            }
                            if (nodeStartOnPage(r0.startContainer)) {
                                freshNode = { node: r0.startContainer, offset: 0 };
                            }
                        }
                        var found = freshNode || fallbackNode;
                        if (found) {
                            window.__quillPageStartNode   = found.node;
                            window.__quillPageStartOffset = found.offset;
                        }
                    })();

                    if (window.__quillScrollLockInstalled) return;
                    window.__quillScrollLockInstalled = true;
                    window.__quillSelectionActive = false;
                    window.__quillSelChanging = false;

                    var lockedScrollLeft = 0;
                    var rafId            = null;
                    var selChangeTimer   = null;
                    var lastCapture      = 0;
                    var lastBridgeText   = "";

                    // Use the raw descriptor to bypass any overrides and avoid triggering
                    // Readium's own scroll listeners when we restore position.
                    var sd = Object.getOwnPropertyDescriptor(Element.prototype,    'scrollLeft')
                          || Object.getOwnPropertyDescriptor(HTMLElement.prototype,'scrollLeft');
                    function getScroll() { return sd ? sd.get.call(scrollEl) : scrollEl.scrollLeft; }
                    function setScroll(v) { if (sd && sd.set) sd.set.call(scrollEl, v); else scrollEl.scrollLeft = v; }

                    // Puts the page back if it has moved at all while text is selected. Chromium's
                    // reveal (when a handle reaches a paragraph begun on the previous page) can
                    // nudge it just a few dozen pixels, which a looser tolerance let through.
                    // Whether this selection began in paged mode (pages laid out side by side, so
                    // the document is wider than the screen). Scroll mode is left as it was.
                    var pagedSelection = false;
                    function isPaged() { return scrollEl.scrollWidth > window.innerWidth + 1; }

                    function holdPage() {
                        if (window.__quillSelectionActive && pagedSelection && Math.abs(getScroll() - lockedScrollLeft) > 0.5) {
                            setScroll(lockedScrollLeft);
                        }
                    }

                    function rafGuard() {
                        if (!window.__quillSelectionActive) { rafId = null; return; }
                        holdPage();
                        rafId = requestAnimationFrame(rafGuard);
                    }

                    // While text is selected, every scroll is instant. A smooth one runs on the
                    // compositor, drawing a few frames of its slide before the guard can stop it.
                    var instantStyle = document.createElement('style');
                    instantStyle.textContent = 'html, body { scroll-behavior: auto !important; }';
                    function setInstantScroll(on) {
                        if (on) {
                            if (!instantStyle.isConnected) (document.head || document.documentElement).appendChild(instantStyle);
                        } else if (instantStyle.isConnected) {
                            instantStyle.remove();
                        }
                    }

                    // Scroll events arrive before the next frame's animation callbacks: catching
                    // a drift here puts the page back that much sooner
                    window.addEventListener('scroll', holdPage, { passive: true, capture: true });

                    document.addEventListener('selectionchange', function() {
                        var sel = window.getSelection();

                        // ── Selection ended ──────────────────────────────────────────────────
                        if (!sel || sel.isCollapsed || sel.rangeCount === 0 || !sel.toString().trim()) {
                            window.__quillSelData = null;
                            window.__quillSelChanging = false;
                            window.__quillSelChangeCount = 0;
                            lastBridgeText = "";
                            if (selChangeTimer) { clearTimeout(selChangeTimer); selChangeTimer = null; }
                            if (window.__quillSelectionActive) {
                                window.__quillSelectionActive = false;
                                setInstantScroll(false);
                            }
                            return;
                        }

                        // ── Selection started ─────────────────────────────────────────────────
                        if (!window.__quillSelectionActive) {
                            pagedSelection = isPaged();
                            if (pagedSelection) setInstantScroll(true);
                            lockedScrollLeft = getScroll();
                            window.__quillSelectionActive = true;
                            if (!rafId) rafId = requestAnimationFrame(rafGuard);
                        } else {
                            // The handle moved: if that pulled the page, put it back right away
                            holdPage();
                        }

                        // ── Bridge Trigger (Zero-lag Haptics) ───────────────────────────────
                        var text = sel.toString();
                        if (text && text !== lastBridgeText) {
                            lastBridgeText = text;
                            // Check if the bridge is available before calling
                            if (window.quillSelection && window.quillSelection.onTextChange) {
                                window.quillSelection.onTextChange(text);
                            }
                        }

                        // ── Capture visible rects for the polling loop (throttled) ───────────
                        // getClientRects() forces a synchronous layout flush. Calling it on
                        // every selectionchange (dozens per second during a handle drag) causes
                        // layout thrashing that stalls the renderer and produces text flickering.
                        // One flush per 100 ms keeps __quillSelData fresh for the fallback.
                        var now = Date.now();
                        if (now - lastCapture >= 100) {
                            lastCapture = now;
                            try {
                                var capRange = sel.getRangeAt(0);
                                var capRects = capRange.getClientRects();
                                var capTol = window.innerWidth * 0.1;
                                var capVis = [];
                                for (var ci = 0; ci < capRects.length; ci++) {
                                    var cr = capRects[ci];
                                    if (cr.left >= -capTol && cr.left < window.innerWidth + capTol) {
                                        capVis.push({top: cr.top, bottom: cr.bottom, left: cr.left, right: cr.right});
                                    }
                                }
                                // Focus is always on the current page — use it as fallback rect
                                if (capVis.length === 0 && sel.focusNode) {
                                    var cfr2 = document.createRange();
                                    var cfo2 = Math.min(sel.focusOffset, sel.focusNode.nodeType === 3 ? sel.focusNode.length : sel.focusNode.childNodes.length);
                                    cfr2.setStart(sel.focusNode, cfo2); cfr2.collapse(true);
                                    var cfRect2 = cfr2.getBoundingClientRect();
                                    if (cfRect2.height > 0) capVis.push({top: cfRect2.top, bottom: cfRect2.bottom, left: cfRect2.left, right: cfRect2.right});
                                }
                                if (capVis.length > 0) {
                                    window.__quillSelData = {text: sel.toString(), rects: capVis};
                                }
                            } catch(capErr) {}
                        }

                        // ── Debounce: mark selection as actively changing ──────────────────────
                        // The toolbar is only shown once this flag clears (once the selection holds still).
                        // The changes in this burst are counted so the poll can tell a handle
                        // drag (many) from the system adjusting a fresh selection (one or two).
                        if (!window.__quillSelChanging) window.__quillSelChangeCount = 0;
                        window.__quillSelChangeCount = (window.__quillSelChangeCount || 0) + 1;
                        window.__quillSelChanging = true;
                        // How long the selection must hold still to count as settled. A fresh
                        // selection (a change or two) settles quickly, so the toolbar comes up
                        // promptly; a handle drag (a run of changes) waits longer, so a brief pause
                        // mid-drag doesn't pop the toolbar up under the finger.
                        var settleMs = window.__quillSelChangeCount >= $DRAG_CHANGE_COUNT ? 250 : 90;
                        if (selChangeTimer) clearTimeout(selChangeTimer);
                        selChangeTimer = setTimeout(function() {
                            window.__quillSelChanging = false;
                            window.__quillSelChangeCount = 0;
                        }, settleMs);
                    });
                })();
            """.trimIndent()
            try { nav.evaluateJavascript(js) } catch (_: Exception) {}
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

private fun computeToolbarY(rectTop: Float, rectBottom: Float, screenHeight: Float): Dp {
    // Require 50dp of breathing room beyond the toolbar height before placing above/below.
    // If the available space is even slightly marginal, overlay over the selection instead.
    val comfortThreshold = TOOLBAR_HEIGHT_DP + 50f
    // Small: a selection's rect is the full line box, and the book's line spacing already
    // leaves room between it and the glyphs, so the toolbar sits close to the words
    val gapAbove = 2f
    val gapBelow = 0f
    val hasRoomAbove = rectTop >= comfortThreshold
    val hasRoomBelow = (screenHeight - rectBottom) >= comfortThreshold
    return when {
        hasRoomAbove -> (rectTop - TOOLBAR_HEIGHT_DP - gapAbove).dp
        hasRoomBelow -> (rectBottom + gapBelow).dp
        // Not enough comfortable space above or below: overlay over the selection centre
        else -> ((rectTop + rectBottom - TOOLBAR_HEIGHT_DP) / 2f)
            .coerceIn(SCREEN_EDGE_MARGIN, screenHeight - TOOLBAR_HEIGHT_DP - SCREEN_EDGE_MARGIN).dp
    }
}
