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

                    // Refresh page-start fallback on every page render/navigation.
                    var startRange = document.caretRangeFromPoint(4, 4);
                    if (startRange) {
                        window.__quillPageStartNode   = startRange.startContainer;
                        window.__quillPageStartOffset = startRange.startOffset;
                    }

                    if (window.__quillScrollLockInstalled) return;
                    window.__quillScrollLockInstalled = true;
                    window.__quillSelectionActive = false;

                    var lockedScrollLeft = 0;
                    var lastValidAnchorNode = null;
                    var lastValidAnchorOffset = 0;
                    var inCorrection = false;

                    var scrollDesc = Object.getOwnPropertyDescriptor(Element.prototype, 'scrollLeft')
                                  || Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollLeft');

                    function getRawScrollLeft() {
                        return scrollDesc ? scrollDesc.get.call(scrollEl) : scrollEl.scrollLeft;
                    }
                    function setRawScrollLeft(v) {
                        if (scrollDesc && scrollDesc.set) scrollDesc.set.call(scrollEl, v);
                        else scrollEl.scrollLeft = v;
                    }

                    // Layer A: block JS-level scrollLeft writes during selection.
                    if (scrollDesc && scrollDesc.set) {
                        Object.defineProperty(scrollEl, 'scrollLeft', {
                            configurable: true,
                            get: function() { return scrollDesc.get.call(this); },
                            set: function(v) {
                                if (window.__quillSelectionActive) return;
                                scrollDesc.set.call(this, v);
                            }
                        });
                    }

                    // Layer B: catch C++ scroll-to-anchor that bypassed Layer A.
                    scrollEl.addEventListener('scroll', function() {
                        if (!window.__quillSelectionActive) return;
                        if (Math.abs(getRawScrollLeft() - lockedScrollLeft) > 1) {
                            setRawScrollLeft(lockedScrollLeft);
                        }
                    }, { passive: false });

                    // Layer C: track the last valid anchor position on the current page.
                    // When the anchor crosses into a previous-page column, restore scroll and
                    // clamp the anchor back to the last known on-page position — NOT to the
                    // fixed page start, which would incorrectly widen the selection.
                    document.addEventListener('selectionchange', function() {
                        if (inCorrection) return;

                        var sel = window.getSelection();
                        if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
                            window.__quillSelectionActive = false;
                            return;
                        }

                        if (!window.__quillSelectionActive) {
                            lockedScrollLeft        = getRawScrollLeft();
                            lastValidAnchorNode     = sel.anchorNode;
                            lastValidAnchorOffset   = sel.anchorOffset;
                            window.__quillSelectionActive = true;
                            return;
                        }

                        // Restore scroll first so the rect check below is meaningful.
                        if (Math.abs(getRawScrollLeft() - lockedScrollLeft) > 1) {
                            setRawScrollLeft(lockedScrollLeft);
                        }

                        // After restoring scroll to lockedScrollLeft, check whether the
                        // anchor is visible within the current page column [0, innerWidth).
                        var anchorOnPage = false;
                        if (sel.anchorNode) {
                            try {
                                var ar = document.createRange();
                                var safeOff = (sel.anchorNode.nodeType === 3)
                                    ? Math.min(sel.anchorOffset, sel.anchorNode.length)
                                    : Math.min(sel.anchorOffset, sel.anchorNode.childNodes.length);
                                ar.setStart(sel.anchorNode, safeOff);
                                ar.collapse(true);
                                var arRect = ar.getBoundingClientRect();
                                anchorOnPage = arRect.left > -4 && arRect.left < window.innerWidth;
                            } catch(e) {}
                        }

                        if (anchorOnPage) {
                            // Anchor is on the current page — update our tracking.
                            lastValidAnchorNode   = sel.anchorNode;
                            lastValidAnchorOffset = sel.anchorOffset;
                        } else {
                            // Anchor has left the current page — clamp back to last valid position.
                            var clampNode   = lastValidAnchorNode   || window.__quillPageStartNode;
                            var clampOffset = lastValidAnchorNode ? lastValidAnchorOffset : (window.__quillPageStartOffset || 0);
                            if (!clampNode || !sel.focusNode) return;
                            try {
                                inCorrection = true;
                                sel.setBaseAndExtent(clampNode, clampOffset, sel.focusNode, sel.focusOffset);
                            } catch(ex) {
                            } finally {
                                inCorrection = false;
                            }
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
