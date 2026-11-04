package com.yugentech.quill.ui.access.signIn.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.ui.access.onboarding.components.OnPaper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Where a reader might have left off: the book, the chapter, a line from there, and how far in. */
private class Place(val book: String, val chapter: String, val line: String, val progress: Float)

/** Coming-back places for sign-in, each a real line from that chapter of a public-domain classic. */
private val Places = listOf(
    Place("Pride and Prejudice", "Chapter 34", "“In vain have I struggled. It will not do.”", 0.56f),
    Place("Jane Eyre", "Chapter 23", "“I am no bird; and no net ensnares me.”", 0.62f),
    Place("Frankenstein", "Chapter 5", "It was on a dreary night of November that I beheld the accomplishment of my toils.", 0.24f),
    Place("Little Women", "Chapter 1", "“Christmas won't be Christmas without any presents,” grumbled Jo.", 0.04f),
    Place("Moby-Dick", "Chapter 1", "Call me Ishmael.", 0.01f)
)

private val PageWidth = 232.dp
private val PageHeight = 148.dp

/** How long each page rests before it lifts away. */
private const val REST_MILLIS = 3200L

/** The lift, as in the onboarding's first step: the page lifting off, and the next settling under. */
private const val LIFT_MILLIS = 460
private const val SETTLE_MILLIS = 420
private const val CONTENT_FADE_MILLIS = 320

/** How the page beneath sits while the top one is at rest: a little smaller, lower and fainter. */
private const val UNDER_SCALE = 0.94f
private val UNDER_DROP = 12.dp
private const val UNDER_ALPHA = 0.6f

private val PageShape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp)

/**
 * Sign-in's opener, as Hajime's flashcard is: a book held open at its bookmark, showing where the
 * reader left off (the book, the chapter, a line from there, how far in), on a small stack. Every
 * few seconds the page lifts away as the onboarding's first step does (off to the left with a
 * slight tilt, fading), the page beneath rises into its place and the next book's place fades in
 * on it. For someone coming back to their reading.
 */
@Composable
fun BookmarkedPage(modifier: Modifier = Modifier) {
    // A page on the aura: in the app's own colours
    OnPaper { BookmarkedPageContent(modifier) }
}

@Composable
private fun BookmarkedPageContent(modifier: Modifier) {
    var index by remember { mutableIntStateOf(0) }
    // 0 = the page at rest on the stack, 1 = lifted away, the page beneath risen into its place
    val lift = remember { Animatable(0f) }
    // The next page beneath, easing into place once the last one has risen: 0 = not there yet
    val underIn = remember { Animatable(1f) }
    // The page's words and ribbon: they fade in on the risen page, which arrives blank
    val contentIn = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(REST_MILLIS)
            lift.animateTo(1f, tween(LIFT_MILLIS, easing = FastOutSlowInEasing))
            // The risen page now looks exactly like the top one at rest, blank: swap them back in
            // the same frame, with the next place on it, hidden, and the next page beneath hidden
            index = (index + 1) % Places.size
            contentIn.snapTo(0f)
            underIn.snapTo(0f)
            lift.snapTo(0f)
            launch { underIn.animateTo(1f, tween(SETTLE_MILLIS, easing = FastOutSlowInEasing)) }
            contentIn.animateTo(1f, tween(CONTENT_FADE_MILLIS))
        }
    }

    val place = Places[index]
    val colors = MaterialTheme.colorScheme

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // The blank page beneath: tucked a little lower and smaller, rising to take the top
        // page's place as it lifts away, then the next one easing in under it
        Surface(
            modifier = Modifier
                .size(PageWidth, PageHeight)
                .graphicsLayer {
                    val p = lift.value
                    val settle = 1f - underIn.value
                    val scale = UNDER_SCALE + (1f - UNDER_SCALE) * p - 0.03f * settle
                    scaleX = scale
                    scaleY = scale
                    translationY = ((1f - p) + settle) * UNDER_DROP.toPx()
                    alpha = (UNDER_ALPHA + (1f - UNDER_ALPHA) * p) * underIn.value
                },
            shape = PageShape,
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {}

        // The page with the place on it, lifting off to the left with a slight tilt as it fades
        Surface(
            modifier = Modifier
                .size(PageWidth, PageHeight)
                .graphicsLayer {
                    val p = lift.value
                    translationX = -p * size.width * 0.45f
                    translationY = -p * 28.dp.toPx()
                    rotationZ = -p * 7f
                    val scale = 1f + 0.03f * p
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - p
                },
            shape = PageShape,
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentIn.value }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 34.dp, top = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = place.book.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = place.chapter,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    Text(
                        text = place.line,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = colors.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    // How far through the book
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.onSurface.copy(alpha = 0.10f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(place.progress.coerceAtLeast(0.03f))
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${(place.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                // The ribbon bookmark, hanging from the top of the page
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 14.dp)
                        .offset(y = (-4).dp)
                        .size(width = 12.dp, height = 40.dp)
                        .clip(RibbonShape)
                        .background(colors.tertiary)
                )
            }
        }
    }
}

/** A ribbon's notched tail. */
private val RibbonShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(size.width / 2f, size.height - size.width * 0.6f)
    lineTo(0f, size.height)
    close()
}
