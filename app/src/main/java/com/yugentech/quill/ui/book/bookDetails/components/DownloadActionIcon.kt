package com.yugentech.quill.ui.book.bookDetails.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** The slot every state's icon sits in, so the label beside it never shifts. */
private val IconSlot = 24.dp

/** How long the arrow takes to fall out of the button when a download starts; what follows waits for it. */
internal const val ARROW_DROP_MILLIS = 300

/** How long the drawn tick stays after a download lands, before the book icon takes over. */
private const val TickHoldMillis = 1400L

/** From the tap to the ring being fully in: the arrow's fall, then the ring growing. */
private const val RING_IN_MILLIS = ARROW_DROP_MILLIS + 350L

/** How long the ring takes to fill the rest of the way once the download is in. */
private const val RING_FILL_MILLIS = 650L

/** How long the full ring rests before the tick takes over. */
private const val FULL_RING_HOLD_MILLIS = 300L

/** The button's state and ring progress as shown, which can trail the real ones a little. */
internal class PacedDownload(val state: MainActionState, val progress: Float?)

/**
 * Paces a download that finishes faster than its animation: the button stays on Downloading
 * until the ring is fully in, fills it to the end, lets it rest a moment, and only then moves on
 * to Read (with the tick, and a tap). Every other change shows straight away.
 */
@Composable
internal fun rememberPacedDownload(state: MainActionState, progress: Float?): PacedDownload {
    val haptic = LocalHapticFeedback.current
    var shown by remember { mutableStateOf(state) }
    var filling by remember { mutableStateOf(false) }
    var ringSince by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state) {
        filling = false
        if (state == MainActionState.Downloading && shown != MainActionState.Downloading) {
            ringSince = SystemClock.uptimeMillis()
        }
        if (shown == MainActionState.Downloading && state == MainActionState.Read) {
            delay((RING_IN_MILLIS - (SystemClock.uptimeMillis() - ringSince)).coerceAtLeast(0L))
            filling = true
            delay(RING_FILL_MILLIS + FULL_RING_HOLD_MILLIS)
            filling = false
            // The download lands with a tap
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        shown = state
    }
    return PacedDownload(shown, if (filling) 1f else progress)
}

/** What the icon shows: one per look, so the same look never animates into itself. */
private enum class IconLook { Arrow, Ring, Tick, Book, Retry, Unavailable }

/**
 * The main action's icon through a download, after the Play Store's Material 3 Expressive one:
 *
 * - **Download:** a download arrow.
 * - **Downloading:** the arrow drops away as a wavy ring grows in its place. The ring waits empty
 *   until the download starts, fills with it ([progress]), and stays full while the book unpacks.
 * - **Downloaded:** if the download finished while the page was open ([justDownloaded]), the ring
 *   bursts into a tick that draws itself in, holds a moment, then gives way to the book icon.
 *   A book already on the device just shows the book icon.
 *
 * Everything takes the button's content colour, so it follows the button through its states.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DownloadActionIcon(
    state: MainActionState,
    progress: Float?,
    justDownloaded: Boolean
) {
    var showTick by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == MainActionState.Read && justDownloaded) {
            showTick = true
            delay(TickHoldMillis)
        }
        showTick = false
    }

    val look = when (state) {
        MainActionState.Download -> IconLook.Arrow
        MainActionState.Downloading -> IconLook.Ring
        MainActionState.Read -> if (showTick) IconLook.Tick else IconLook.Book
        MainActionState.Retry -> IconLook.Retry
        MainActionState.Unavailable -> IconLook.Unavailable
    }
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    AnimatedContent(
        targetState = look,
        transitionSpec = { transitionFor(initialState, targetState) using null },
        contentAlignment = Alignment.Center,
        label = "downloadActionIcon",
        modifier = Modifier.size(IconSlot)
    ) { target ->
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(IconSlot)) {
            when (target) {
                IconLook.Arrow -> Icon(Icons.Rounded.Download, null, Modifier.size(22.dp))
                IconLook.Ring -> DownloadRing(progress = progress)
                IconLook.Tick -> DrawnTick()
                IconLook.Book -> Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(20.dp))
                IconLook.Retry -> Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                IconLook.Unavailable -> Icon(Icons.Rounded.Construction, null, Modifier.size(20.dp))
            }
        }
    }
}

/** The handoffs that carry the download's story move; anything else fades through. */
private fun transitionFor(from: IconLook, to: IconLook): ContentTransform = when {
    // The arrow falls all the way out of the button, solid (the button's shape clips it), and
    // only then does the ring grow where it was
    from == IconLook.Arrow && to == IconLook.Ring ->
        (scaleIn(tween(350, delayMillis = ARROW_DROP_MILLIS, easing = FastOutSlowInEasing), initialScale = 0.3f) +
            fadeIn(tween(200, delayMillis = ARROW_DROP_MILLIS))) togetherWith
            slideOutVertically(tween(ARROW_DROP_MILLIS, easing = FastOutLinearInEasing)) { it * 2 }

    // The ring swells away and the tick pops in with a little bounce
    from == IconLook.Ring && to == IconLook.Tick ->
        scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), initialScale = 0.4f) +
            fadeIn(tween(150)) togetherWith
            (scaleOut(tween(250), targetScale = 1.4f) + fadeOut(tween(200)))

    // A download cancelled or failed: the arrow (or retry) comes back up from below
    from == IconLook.Ring ->
        (slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(250))) togetherWith
            (scaleOut(tween(200), targetScale = 0.4f) + fadeOut(tween(200)))

    else -> fadeIn(tween(220, delayMillis = 90)) + scaleIn(tween(220, delayMillis = 90), initialScale = 0.92f) togetherWith
        fadeOut(tween(90))
}

/**
 * The wavy ring of a running download, always the filling kind. [progress] is null before the
 * first bytes arrive, so the ring waits empty; it's null again once the bytes are all in and the
 * book is being unpacked, so the ring fills the rest of the way and holds there.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadRing(progress: Float?) {
    val color = LocalContentColor.current
    val trackColor = color.copy(alpha = 0.25f)
    val strokeWidth = with(LocalDensity.current) { 2.5.dp.toPx() }
    val stroke = remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
    // Whether the download has reported any progress yet, for telling "not started" from "done"
    var started by remember { mutableStateOf(false) }
    if (progress != null && !started) started = true
    val target = progress ?: if (started) 1f else 0f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "downloadRingProgress"
    )

    CircularWavyProgressIndicator(
        progress = { animated },
        modifier = Modifier.size(22.dp),
        color = color,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = stroke,
        gapSize = 2.dp,
        wavelength = 7.dp
    )
}

/** A tick that draws itself from its short stroke to the end of its long one. */
@Composable
private fun DrawnTick() {
    val color = LocalContentColor.current
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        draw.animateTo(1f, tween(420, delayMillis = 80, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.size(22.dp)) {
        val unit = size.minDimension / 24f
        // A soft disc behind it, so the tick lands on something
        drawCircle(color = color.copy(alpha = 0.18f), radius = size.minDimension / 2f)

        val full = Path().apply {
            moveTo(6.5f * unit, 12.5f * unit)
            lineTo(10.5f * unit, 16.5f * unit)
            lineTo(17.5f * unit, 8f * unit)
        }
        val measure = PathMeasure().apply { setPath(full, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * draw.value, partial, true)
        drawPath(
            path = partial,
            color = color,
            style = Stroke(width = 2.5f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
