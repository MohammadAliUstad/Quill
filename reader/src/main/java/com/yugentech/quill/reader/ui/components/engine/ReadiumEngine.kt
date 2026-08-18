package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yugentech.quill.reader.viewmodel.ReaderCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
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

private const val TOOLBAR_HEIGHT_DP = 52f
private const val TOOLBAR_MARGIN_DP = 8f

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
    val fragmentTag = remember(bookId, preferences.scroll) {
        "readium_${bookId}_${if (preferences.scroll == true) "scroll" else "paged"}"
    }

    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val cssColorString = remember(selectionColor) { selectionColor.toCssRgba() }

    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var selectionInfo by remember { mutableStateOf<SelectionInfo?>(null) }
    var toolbarY by remember { mutableStateOf(0.dp) }

    val scope = rememberCoroutineScope()
    val isPagedMode = preferences.scroll != true

    // Active selection decoration for flicker-free paged mode
    val activeSelectionDecoration = remember(selectionInfo) {
        val loc = selectionInfo?.locator ?: return@remember null
        Decoration(
            id = "active_selection",
            locator = loc,
            style = Decoration.Style.Highlight(tint = selectionColor.toArgb())
        )
    }

    suspend fun clearSelection() {
        val nav = navigator ?: return
        try {
            nav.evaluateJavascript("window.__quillSelData = null; window.getSelection().removeAllRanges();")
        } catch (_: Exception) {}
        selectionInfo = null
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
            val rects = obj.optJSONArray("rects")
            val locator = nav.currentSelection()?.locator
            
            if (rects != null && rects.length() > 0 && text.isNotEmpty()) {
                val first = rects.getJSONObject(0)
                val last = rects.getJSONObject(rects.length() - 1)
                selectionInfo = SelectionInfo(
                    text = text,
                    locator = locator,
                    rectTop = first.getDouble("top").toFloat(),
                    rectBottom = last.getDouble("bottom").toFloat()
                )
                toolbarY = computeToolbarY(selectionInfo!!.rectTop, selectionInfo!!.rectBottom)
            } else {
                selectionInfo = null
            }
        } catch (e: Exception) { 
            selectionInfo = null
        }
    }

    val density = LocalDensity.current.density
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = modifier.fillMaxSize()) {
        ReadiumFragmentHost(
            publication = publication,
            fragmentTag = fragmentTag,
            initialLocation = initialLocation,
            preferences = preferences,
            isPro = isPro,
            isAiraReady = isAiraReady,
            onAskAira = { text ->
                onAskAira(text)
                scope.launch { clearSelection() }
            },
            onHighlightRequest = { locator ->
                onSelectionAction(locator)
                scope.launch { clearSelection() }
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
            }
        )
    }

    val currentSelection = selectionInfo
    if (currentSelection != null) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, (toolbarY.value * density).roundToInt()),
            properties = PopupProperties(focusable = false)
        ) {
            SelectionToolbar(
                selectionInfo = currentSelection,
                isAiraReady = isAiraReady,
                onHighlight = {
                    currentSelection.locator?.let { onSelectionAction(it) }
                    scope.launch { clearSelection() }
                },
                onAskAira = { text ->
                    onAskAira(text)
                    scope.launch { clearSelection() }
                },
                onCopy = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                    scope.launch { clearSelection() }
                }
            )
        }
    }

    LaunchedEffect(decorations, activeSelectionDecoration, navigator) {
        val decorableNav = navigator as? DecorableNavigator
        decorableNav?.applyDecorations(decorations, "user_highlights")
        
        if (isPagedMode) {
            val selectionList = if (activeSelectionDecoration != null) listOf(activeSelectionDecoration) else emptyList()
            decorableNav?.applyDecorations(selectionList, "active_selection_group")
        }
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

    // Polling Loop for Selection Info (Transparent native selection tracking)
    LaunchedEffect(navigator) {
        val nav = navigator ?: return@LaunchedEffect
        while (true) {
            delay(150)
            val json = try {
                nav.evaluateJavascript("""
                    (function() {
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
                                var liveData = { text: sel.toString(), rects: filteredRects };
                                window.__quillSelData = liveData;
                                return liveData;
                            }
                        }
                        // If live selection is gone or produced no rects, use stored capture from
                        // selectionchange — handles the case where Readium collapsed the selection
                        // before the poll ran (cross-page anchor on a continuation paragraph).
                        return window.__quillSelData || null;
                    })()
                """.trimIndent())
            } catch (_: Exception) { null }
            syncSelection(json?.removeSurrounding("\""))
        }
    }

    LaunchedEffect(navigator, cssColorString) {
        val nav = navigator ?: return@LaunchedEffect

        suspend fun injectSelectionStyles() {
            // In paged mode, we make the native selection transparent and use Readium Decorations
            // to draw the visual highlight. This eliminates the flickering handles issue.
            val bgColor = cssColorString
            val js = """
                (function() {
                    var id = 'quill-selection-style';
                    var existing = document.getElementById(id);
                    if (existing) existing.remove();
                    var s = document.createElement('style');
                    s.id = id;
                    s.textContent = `
                        ::selection {
                            background-color: $bgColor !important;
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

                    var lockedScrollLeft = 0;
                    var rafId            = null;

                    // Use the raw descriptor to bypass any overrides and avoid triggering
                    // Readium's own scroll listeners when we restore position.
                    var sd = Object.getOwnPropertyDescriptor(Element.prototype,    'scrollLeft')
                          || Object.getOwnPropertyDescriptor(HTMLElement.prototype,'scrollLeft');
                    function getScroll() { return sd ? sd.get.call(scrollEl) : scrollEl.scrollLeft; }
                    function setScroll(v) { if (sd && sd.set) sd.set.call(scrollEl, v); else scrollEl.scrollLeft = v; }

                    // requestAnimationFrame guard — corrects scroll BEFORE each frame is painted.
                    // Handles the case where the browser scrolled to reveal an off-page anchor.
                    function rafGuard() {
                        if (!window.__quillSelectionActive) { rafId = null; return; }
                        if (Math.abs(getScroll() - lockedScrollLeft) > 1) setScroll(lockedScrollLeft);
                        rafId = requestAnimationFrame(rafGuard);
                    }

                    document.addEventListener('selectionchange', function() {
                        var sel = window.getSelection();

                        // ── Selection ended ──────────────────────────────────────────────────
                        if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
                            if (window.__quillSelectionActive) {
                                window.__quillSelectionActive = false;
                                // rafGuard self-terminates on next tick
                            }
                            return;
                        }

                        // ── Selection started ─────────────────────────────────────────────────
                        if (!window.__quillSelectionActive) {
                            lockedScrollLeft = getScroll();
                            window.__quillSelectionActive = true;
                            if (!rafId) rafId = requestAnimationFrame(rafGuard);
                        }

                        // ── Capture visible rects for the polling loop ────────────────────────
                        // Stored in __quillSelData so polling can use it even if the DOM
                        // selection collapses before the 150 ms tick (cross-page anchors).
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

                        // ── Restore scroll ────────────────────────────────────────────────────
                        if (Math.abs(getScroll() - lockedScrollLeft) > 1) setScroll(lockedScrollLeft);
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

private fun computeToolbarY(rectTop: Float, rectBottom: Float): Dp {
    val hasRoomAbove = rectTop > TOOLBAR_HEIGHT_DP + TOOLBAR_MARGIN_DP + 8f
    return if (hasRoomAbove) {
        (rectTop - TOOLBAR_HEIGHT_DP - TOOLBAR_MARGIN_DP).dp
    } else {
        (rectBottom + TOOLBAR_MARGIN_DP).dp
    }
}
