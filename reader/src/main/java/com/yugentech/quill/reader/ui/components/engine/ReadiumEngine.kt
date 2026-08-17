package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current.density
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
            nav.evaluateJavascript("window.getSelection().removeAllRanges();")
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
            
            if (locator != null && rects != null && rects.length() > 0) {
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

    Box(modifier = modifier.fillMaxSize()) {
        ReadiumFragmentHost(
            publication = publication,
            fragmentTag = fragmentTag,
            initialLocation = initialLocation,
            preferences = preferences,
            isPro = isPro,
            isAiraReady = isAiraReady,
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

        AnimatedVisibility(
            visible = selectionInfo != null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (toolbarY.value * density).roundToInt()) }
        ) {
            selectionInfo?.let { info ->
                SelectionToolbar(
                    selectionInfo = info,
                    isAiraReady = isAiraReady,
                    onHighlight = {
                        scope.launch {
                            val locator = info.locator
                            if (locator != null) onSelectionAction(locator)
                            clearSelection()
                        }
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
                        if (sel.isCollapsed || sel.rangeCount === 0) return null;
                        var range = sel.getRangeAt(0);
                        var rects = range.getClientRects();
                        var filteredRects = [];
                        for (var i = 0; i < rects.length; i++) {
                            var r = rects[i];
                            if (r.left >= 0 && r.left < window.innerWidth) {
                                filteredRects.push({top: r.top, bottom: r.bottom, left: r.left, right: r.right});
                            }
                        }
                        if (filteredRects.length === 0) return null;
                        return JSON.stringify({
                            text: sel.toString(),
                            rects: filteredRects
                        });
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

                    // Always refresh page-start fallback on every page render/navigation.
                    var s0 = document.caretRangeFromPoint(4, 4);
                    if (s0) {
                        window.__quillPageStartNode   = s0.startContainer;
                        window.__quillPageStartOffset = s0.startOffset;
                    }

                    if (window.__quillScrollLockInstalled) return;
                    window.__quillScrollLockInstalled = true;
                    window.__quillSelectionActive = false;

                    var lockedScrollLeft       = 0;
                    var lastValidAnchorNode    = null, lastValidAnchorOffset = 0;
                    var lastValidFocusNode     = null, lastValidFocusOffset  = 0;
                    var inCorrection           = false;
                    var rafId                  = null;

                    // Use the raw descriptor to bypass any overrides and avoid triggering
                    // Readium's own scroll listeners when we restore position.
                    var sd = Object.getOwnPropertyDescriptor(Element.prototype,    'scrollLeft')
                          || Object.getOwnPropertyDescriptor(HTMLElement.prototype,'scrollLeft');
                    function getScroll() { return sd ? sd.get.call(scrollEl) : scrollEl.scrollLeft; }
                    function setScroll(v) { if (sd && sd.set) sd.set.call(scrollEl, v); else scrollEl.scrollLeft = v; }

                    // Returns true when a text/element node sits within the current page column.
                    // Uses a generous tolerance (10 % of viewport width) so sub-pixel rounding
                    // and text near the left margin don't produce false negatives.  Only content
                    // hundreds of pixels off-screen (previous CSS column) will return false.
                    function nodeOnPage(node, off) {
                        if (!node) return false;
                        try {
                            var r = document.createRange();
                            var safeOff = node.nodeType === 3
                                ? Math.min(off, node.length)
                                : Math.min(off, node.childNodes.length);
                            r.setStart(node, safeOff);
                            r.collapse(true);
                            var rect = r.getBoundingClientRect();
                            var tol = window.innerWidth * 0.1;
                            return rect.left > -tol && rect.left < window.innerWidth + tol;
                        } catch(e) { return false; }
                    }

                    // requestAnimationFrame guard — runs BEFORE each frame's paint in Chrome's
                    // rendering pipeline (input → JS → style → layout → rAF → paint/composite).
                    // If the browser scrolled to reveal an off-page anchor during event handling,
                    // this corrects scroll BEFORE the frame is committed to the screen, limiting
                    // any visible flicker to at most one 16 ms frame.
                    // Does NOT mutate the Selection here — selection clamping is done exclusively
                    // in selectionchange so there is exactly one place that calls setBaseAndExtent
                    // and no re-entrant feedback loops are possible.
                    function rafGuard() {
                        if (!window.__quillSelectionActive) { rafId = null; return; }
                        if (Math.abs(getScroll() - lockedScrollLeft) > 1) setScroll(lockedScrollLeft);
                        rafId = requestAnimationFrame(rafGuard);
                    }

                    document.addEventListener('selectionchange', function() {
                        if (inCorrection) return;
                        var sel = window.getSelection();

                        // ── Selection ended ──────────────────────────────────────────────────
                        if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
                            if (window.__quillSelectionActive) {
                                window.__quillSelectionActive = false;
                                // rafGuard self-terminates on next tick
                            }
                            return;
                        }

                        // ── Selection just started ───────────────────────────────────────────
                        if (!window.__quillSelectionActive) {
                            lockedScrollLeft      = getScroll();
                            lastValidAnchorNode   = sel.anchorNode; lastValidAnchorOffset = sel.anchorOffset;
                            lastValidFocusNode    = sel.focusNode;  lastValidFocusOffset  = sel.focusOffset;
                            window.__quillSelectionActive = true;
                            if (!rafId) rafId = requestAnimationFrame(rafGuard);
                            return;
                        }

                        // ── Selection active — restore scroll first so rect checks are accurate ──
                        if (Math.abs(getScroll() - lockedScrollLeft) > 1) setScroll(lockedScrollLeft);

                        var aOk = nodeOnPage(sel.anchorNode, sel.anchorOffset);
                        var fOk = nodeOnPage(sel.focusNode,  sel.focusOffset);

                        if (aOk && fOk) {
                            lastValidAnchorNode = sel.anchorNode; lastValidAnchorOffset = sel.anchorOffset;
                            lastValidFocusNode  = sel.focusNode;  lastValidFocusOffset  = sel.focusOffset;
                            return;
                        }

                        // Clamp only the offending endpoint; keep the other end live.
                        var ca  = aOk ? sel.anchorNode   : (lastValidAnchorNode || window.__quillPageStartNode);
                        var cao = aOk ? sel.anchorOffset : (lastValidAnchorNode ? lastValidAnchorOffset : (window.__quillPageStartOffset || 0));
                        var cf  = fOk ? sel.focusNode    : (lastValidFocusNode  || window.__quillPageStartNode);
                        var cfo = fOk ? sel.focusOffset  : (lastValidFocusNode  ? lastValidFocusOffset  : (window.__quillPageStartOffset || 0));

                        if (!ca || !cf) return;
                        try {
                            inCorrection = true;
                            sel.setBaseAndExtent(ca, cao, cf, cfo);
                        } catch(ex) {
                        } finally {
                            inCorrection = false;
                        }
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
