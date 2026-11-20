package com.yugentech.quill.ui.sources.standard.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * How a source's grid of covers arrives when the screen opens (or a new search replaces it).
 *
 * The covers settle in one after another in reading order, left to right and row by row: each
 * fades in while rising a few dp into its cell, so the grid fills like a ripple running down
 * the page rather than zooming in as one block. The whole thing is over in about half a second.
 *
 * It plays once. A cover first composed after the opening window (scrolled into view, or from a
 * later page) just appears, so scrolling and paging never replay it.
 *
 * It only plays for books that had to be fetched, arriving after the skeleton. Books already
 * cached are simply there, with nothing to wait for ([LocalGridEntranceEnabled]).
 */
@Stable
class GridEntrance internal constructor(private val startedAt: Long, private val plays: Boolean) {

    /** How long to wait before [index] arrives, or null when it should just be there. */
    internal fun delayFor(index: Int): Long? {
        if (!plays) return null
        val elapsed = SystemClock.uptimeMillis() - startedAt
        if (elapsed > ENTRANCE_WINDOW_MS) return null
        val due = index.coerceAtMost(MAX_STAGGERED) * STAGGER_MS
        return (due - elapsed).coerceAtLeast(0L)
    }

    private companion object {
        /** Each cover starts this long after the one before it. */
        const val STAGGER_MS = 35L

        /** Covers past this many start together with it, so a tall screen doesn't drag. */
        const val MAX_STAGGERED = 15

        /** Covers composed later than this after the grid appeared don't animate at all. */
        const val ENTRANCE_WINDOW_MS = 700L
    }
}

/**
 * A fresh entrance each time [key] changes: pass something that identifies the list, such as the
 * first book's ID, so a new search ripples in again while pages added beneath don't.
 */
@Composable
fun rememberGridEntrance(key: Any?): GridEntrance {
    val plays = LocalGridEntranceEnabled.current
    return remember(key) { GridEntrance(SystemClock.uptimeMillis(), plays) }
}

/**
 * Whether a grid composed here ripples in: true for books that replaced the skeleton, false for
 * books shown straight from the cache. Set by [SourceContentHost].
 */
val LocalGridEntranceEnabled = staticCompositionLocalOf { true }

/** The cover at [index] taking part in [entrance]: fading in and rising into its cell on its turn. */
fun Modifier.gridEntrance(entrance: GridEntrance, index: Int): Modifier = composed {
    val wait = remember(entrance) { entrance.delayFor(index) }
    val progress = remember(entrance) { Animatable(if (wait == null) 1f else 0f) }
    val rise = with(LocalDensity.current) { ENTRANCE_RISE.toPx() }

    LaunchedEffect(entrance) {
        if (wait == null) return@LaunchedEffect
        delay(wait)
        // Settles without a bounce: the covers are content, not a flourish
        progress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
    }

    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * rise
    }
}

/** How far below its cell a cover starts. */
private val ENTRANCE_RISE = 16.dp
