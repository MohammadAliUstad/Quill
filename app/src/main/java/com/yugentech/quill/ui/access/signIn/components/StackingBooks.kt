package com.yugentech.quill.ui.access.signIn.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.ui.access.onboarding.components.paperColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A book lying on the pile: its title, how long it is and how it sits. */
private class PiledBook(val title: String, val width: Dp, val shift: Dp, val tilt: Float)

/** First piles, all free public-domain classics: what a new library starts with. */
private val Piles = listOf(
    listOf(
        PiledBook("The Odyssey", 184.dp, (-6).dp, -1.5f),
        PiledBook("Frankenstein", 168.dp, 8.dp, 2f),
        PiledBook("Emma", 152.dp, (-4).dp, -1f),
        PiledBook("Dracula", 160.dp, 6.dp, 2.5f)
    ),
    listOf(
        PiledBook("Moby-Dick", 188.dp, 4.dp, 1.5f),
        PiledBook("Jane Eyre", 160.dp, (-8).dp, -2f),
        PiledBook("Little Women", 172.dp, 6.dp, 1f),
        PiledBook("Walden", 148.dp, (-2).dp, -2.5f)
    ),
    listOf(
        PiledBook("Middlemarch", 186.dp, (-4).dp, -1f),
        PiledBook("The Iliad", 164.dp, 6.dp, 2f),
        PiledBook("Persuasion", 156.dp, (-6).dp, -1.5f),
        PiledBook("Ivanhoe", 166.dp, 4.dp, 2f)
    )
)

/**
 * What's said under each finished pile, a different line every time. There are more lines than
 * piles and they're counted separately, so a pile rarely comes back with the same words.
 */
private val Captions = listOf(
    "Your library begins",
    "One shelf at a time",
    "Classics, free to keep",
    "A pile worth reading",
    "Room for one more",
    "Every reader starts somewhere",
    "Stories waiting for you"
)

private val BookHeight = 26.dp
private const val PILE_SIZE = 4

/** Time between one book landing and the next. */
private const val BOOK_STAGGER_MILLIS = 380L
/** How long the finished pile rests, caption showing, before the books lift away. */
private const val HOLD_MILLIS = 1800L
private const val LIFT_MILLIS = 260

/**
 * Sign-up's opener, as Hajime's spelling tiles are: books drop one at a time onto a growing pile,
 * each landing with a little bounce, then a line fades in underneath ("Your library begins", "One
 * shelf at a time", … a different one each time) before they
 * lift away for the next pile. A first library, built a book at a time.
 */
@Composable
fun StackingBooks(modifier: Modifier = Modifier) {
    // How many piles have been built so far: picks both the pile and, separately, its caption
    var round by remember { mutableIntStateOf(0) }
    var showCaption by remember { mutableStateOf(false) }
    val drops = remember { List(PILE_SIZE) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        while (true) {
            for (i in 0 until PILE_SIZE) {
                launch { drops[i].animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)) }
                delay(BOOK_STAGGER_MILLIS)
            }
            delay(BOOK_STAGGER_MILLIS)
            showCaption = true
            delay(HOLD_MILLIS)
            showCaption = false
            drops.forEach { drop -> launch { drop.animateTo(0f, tween(LIFT_MILLIS)) } }
            delay(LIFT_MILLIS + 120L)
            // The caption is hidden here, so its words change unseen
            round++
        }
    }

    val pile = Piles[round % Piles.size]
    val caption = Captions[round % Captions.size]
    // The books are in the app's own colours; the caption under them is white on the aura
    val colors = paperColors()
    val covers = listOf(
        colors.primary to colors.onPrimary,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        colors.secondary to colors.onSecondary,
        colors.surface to colors.onSurface
    )
    val captionAlpha by animateFloatAsState(if (showCaption) 1f else 0f, tween(300), label = "pileCaption")

    // Fixed height, so the greeting below doesn't move as the pile builds.
    Column(
        modifier = modifier.height(148.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(BookHeight * PILE_SIZE + 4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            pile.forEachIndexed { i, book ->
                val (cover, ink) = covers[i % covers.size]
                LyingBook(
                    book = book,
                    cover = cover,
                    ink = ink,
                    level = i,
                    drop = drops[i].value
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier.graphicsLayer { alpha = captionAlpha }
        )
    }
}

/** One book on the pile, [level] books up; [drop] runs 0 (above, invisible) to 1 (landed), overshooting on the bounce. */
@Composable
private fun LyingBook(book: PiledBook, cover: Color, ink: Color, level: Int, drop: Float) {
    Surface(
        modifier = Modifier
            .size(width = book.width, height = BookHeight)
            .graphicsLayer {
                alpha = drop.coerceIn(0f, 1f)
                translationX = book.shift.toPx()
                translationY = -level * BookHeight.toPx() + (1f - drop) * -36.dp.toPx()
                rotationZ = book.tilt * drop
            },
        shape = RoundedCornerShape(4.dp),
        color = cover,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = book.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
