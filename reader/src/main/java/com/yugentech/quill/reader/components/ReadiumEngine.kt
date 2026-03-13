package com.yugentech.quill.reader.components

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import kotlin.math.abs
import kotlin.math.roundToInt

// Threshold: User must drag 175px to trigger the chapter change
private const val SWIPE_ACTION_THRESHOLD = 175f

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
    onAskAira: (String) -> Unit = {}, // <-- ADDED: Handle text selection intent
    onJumpComplete: () -> Unit,
    onSeekComplete: () -> Unit,
    onLocatorChange: (Locator) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val fragmentTag = remember(bookId) { "readium_$bookId" }

    var swipeOffset by remember { mutableFloatStateOf(0f) }

    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val cssColorString = remember(selectionColor) { selectionColor.toCssRgba() }

    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    val chapterProgressMap = remember { mutableMapOf<String, Locator>() }
    var latestLocator by remember { mutableStateOf<Locator?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ReadiumFragmentHost(
            publication = publication,
            fragmentTag = fragmentTag,
            initialLocation = initialLocation,
            preferences = preferences,
            onTap = onTap,
            onAskAira = onAskAira, // <-- PASSING IT DOWN: To FragmentHost
            onNavigatorReady = { nav ->
                navigator = nav
                setupHorizontalNavigation(
                    navigator = nav,
                    scope = scope,
                    chapterProgressMap = chapterProgressMap,
                    latestLocatorProvider = { latestLocator },
                    onDragChange = { swipeOffset = it }
                )
            }
        )

        ChapterSwipeIndicator(
            swipeOffset = swipeOffset,
            threshold = SWIPE_ACTION_THRESHOLD
        )
    }

    LaunchedEffect(preferences, navigator) { navigator?.submitPreferences(preferences) }

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
                latestLocator = locator
                onLocatorChange(locator)
            }
        }
    }
}

// --- UI COMPONENT: Horizontal Row Indicator ---

@Composable
private fun BoxScope.ChapterSwipeIndicator(
    swipeOffset: Float,
    threshold: Float
) {
    val density = LocalDensity.current

    val isPullingRight = swipeOffset > 0
    val absOffset = abs(swipeOffset)

    if (absOffset < 20f) return

    val alpha by animateFloatAsState(
        targetValue = (absOffset / threshold).coerceIn(0f, 1f),
        label = "Alpha"
    )

    val isReady = absOffset >= threshold

    val targetPillColor = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val pillColor by animateColorAsState(
        targetValue = targetPillColor,
        animationSpec = tween(durationMillis = 300),
        label = "PillColor"
    )

    val targetTextColor = if (isReady) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 300),
        label = "TextColor"
    )

    val text = if (isPullingRight) "Previous Chapter" else "Next Chapter"
    val align = if (isPullingRight) Alignment.CenterStart else Alignment.CenterEnd

    val nudge = (absOffset * 0.1f).coerceAtMost(32f)
    val offsetX = if (isPullingRight) nudge else -nudge

    Box(
        modifier = Modifier
            .align(align)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .padding(horizontal = 16.dp)
            .alpha(alpha)
            .background(pillColor, RoundedCornerShape(50))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isReady) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                val progress = (absOffset / threshold).coerceIn(0f, 1f)
                CircularProgressIndicator(
                    progress = { progress },
                    color = textColor,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * Touch handler for chapter swiping.
 *
 * Progress saving strategy:
 * - At the moment ACTION_UP fires (before any navigation), we snapshot
 *   [latestLocatorProvider] into [chapterProgressMap] for the departing chapter.
 *   This is the critical step — it captures the true reading position rather
 *   than relying on collectLatest which may emit Readium's landing locator
 *   (chapter start or end) and overwrite our saved position.
 * - After navigating, we wait for Readium to load the new chapter, then check
 *   if we have a saved position for it and restore it via nav.go().
 */
@SuppressLint("ClickableViewAccessibility")
private fun setupHorizontalNavigation(
    navigator: EpubNavigatorFragment,
    scope: CoroutineScope,
    chapterProgressMap: MutableMap<String, Locator>,
    latestLocatorProvider: () -> Locator?,
    onDragChange: (Float) -> Unit
) {
    val fragmentView = navigator.view ?: return

    fun findAllWebViews(view: View, result: MutableList<WebView>) {
        if (view is WebView) result.add(view)
        else if (view is ViewGroup) {
            for (i in 0 until view.childCount) findAllWebViews(view.getChildAt(i), result)
        }
    }

    fragmentView.post {
        val webViews = mutableListOf<WebView>()
        findAllWebViews(fragmentView, webViews)

        webViews.forEach { webView ->
            var startX = 0f
            var startY = 0f
            var lastX = 0f
            var accumulatedDrag = 0f
            var isDraggingHorizontal = false

            webView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        lastX = event.x
                        accumulatedDrag = 0f
                        isDraggingHorizontal = false
                        onDragChange(0f)

                        v.parent.requestDisallowInterceptTouchEvent(false)
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.x - lastX
                        lastX = event.x

                        val totalDiffX = abs(event.x - startX)
                        val totalDiffY = abs(event.y - startY)

                        if (!isDraggingHorizontal) {
                            if (totalDiffX > totalDiffY && totalDiffX > 30f) {
                                isDraggingHorizontal = true
                                v.parent.requestDisallowInterceptTouchEvent(true)

                                val cancel = MotionEvent.obtain(event)
                                cancel.action = MotionEvent.ACTION_CANCEL
                                v.onTouchEvent(cancel)
                                cancel.recycle()
                            }
                        }

                        if (isDraggingHorizontal) {
                            accumulatedDrag += deltaX * 0.6f
                            val displayDrag = accumulatedDrag
                                .coerceIn(-SWIPE_ACTION_THRESHOLD, SWIPE_ACTION_THRESHOLD)
                            onDragChange(displayDrag)
                            return@setOnTouchListener true
                        }

                        false
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDraggingHorizontal) {
                            val triggered = abs(accumulatedDrag) > SWIPE_ACTION_THRESHOLD
                            if (triggered) {
                                val goForward = accumulatedDrag < 0

                                // ── Save departure position ──────────────────────────────
                                // Grab the locator RIGHT NOW before Readium moves anywhere.
                                // This is the exact page the user was on, not a landing page.
                                val departureLocator = latestLocatorProvider()
                                val departureHref = departureLocator?.href?.toString()
                                if (departureLocator != null && departureHref != null) {
                                    chapterProgressMap[departureHref] = departureLocator
                                }
                                // ────────────────────────────────────────────────────────

                                scope.launch {
                                    delay(120)
                                    onDragChange(0f)

                                    if (goForward) navigator.goForward(animated = true)
                                    else navigator.goBackward(animated = true)

                                    // Wait for Readium to land on the new chapter, then
                                    // restore the saved position if one exists for it.
                                    // We poll instead of using a fixed delay so we don't
                                    // restore before the chapter has actually changed.
                                    val maxWaitMs = 1500L
                                    val pollIntervalMs = 50L
                                    var waited = 0L
                                    while (waited < maxWaitMs) {
                                        delay(pollIntervalMs)
                                        waited += pollIntervalMs
                                        val arrivedHref = latestLocatorProvider()?.href?.toString()
                                        if (arrivedHref != null && arrivedHref != departureHref) {
                                            // We're on the new chapter — restore saved position.
                                            val savedLocator = chapterProgressMap[arrivedHref]
                                            if (savedLocator != null) {
                                                navigator.go(savedLocator, animated = false)
                                            }
                                            break
                                        }
                                    }
                                }
                            } else {
                                onDragChange(0f)
                            }
                        } else {
                            onDragChange(0f)
                        }

                        accumulatedDrag = 0f
                        isDraggingHorizontal = false
                        false
                    }
                    else -> false
                }
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