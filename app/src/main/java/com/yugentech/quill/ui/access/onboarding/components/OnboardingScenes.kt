package com.yugentech.quill.ui.access.onboarding.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.quill.ui.main.components.pressClickable
import com.yugentech.quill.ui.shared.airaChat.components.TightText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// ---------------------------------------------------------------- Shared pieces

/**
 * A 0 → 1 entrance that starts [delayMs] after the scene becomes [active], on a soft spring
 * (it overshoots a touch, so things settle rather than stop).
 */
@Composable
private fun rememberEntrance(active: Boolean, delayMs: Int): Animatable<Float, *> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            delay(delayMs.toLong())
            progress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
        }
    }
    return progress
}

/** Fades in and rises [rise] into place as [progress] runs 0 → 1. */
private fun Modifier.rise(progress: () -> Float, rise: Dp = 24.dp): Modifier = graphicsLayer {
    val t = progress()
    alpha = t.coerceIn(0f, 1f)
    translationY = (1f - t) * rise.toPx()
}

/** A scene's headline and the line under it, rising in after [delayMs]. */
@Composable
private fun SceneText(title: String, body: String, active: Boolean, delayMs: Int) {
    val titleIn = rememberEntrance(active, delayMs)
    val bodyIn = rememberEntrance(active, delayMs + 140)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.rise({ titleIn.value })
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .rise({ bodyIn.value })
        )
    }
}

/** A small pill with an icon, for hints. */
@Composable
private fun Pill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    // A soft wash of the primary over the aura, with a faint primary edge: light in light themes
    // and dark ones alike (a surface chip turns near-black in a dark theme)
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = CircleShape,
        color = primary.copy(alpha = 0.12f),
        contentColor = primary,
        border = BorderStroke(1.dp, primary.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Every scene's frame: centred, with room at the sides. */
@Composable
private fun SceneColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

// ---------------------------------------------------------------- 1. The first line

private class Opening(val line: String, val source: String)

/** Famous first lines, all from public-domain classics Quill's free library carries. */
private val Openings = listOf(
    Opening("It was the best of times, it was the worst of times.", "Charles Dickens, A Tale of Two Cities"),
    Opening("Call me Ishmael.", "Herman Melville, Moby-Dick"),
    Opening("It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.", "Jane Austen, Pride and Prejudice"),
    Opening("All happy families are alike; each unhappy family is unhappy in its own way.", "Leo Tolstoy, Anna Karenina")
)

/**
 * A page with a famous first line on it, written in word by word, on a small stack. Tap it and
 * the page lifts off the stack, the one beneath rises into its place, and the next great opening
 * is written onto it.
 */
@Composable
internal fun FirstLineScene(active: Boolean) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pageIn = rememberEntrance(active, 150)
    val hintIn = rememberEntrance(active, 2200)
    var index by remember { mutableIntStateOf(0) }
    // 0 = the page at rest on the stack, 1 = lifted away, the page beneath risen into its place
    val lift = remember { Animatable(0f) }
    // The next page beneath, easing into place once the last one has risen: 0 = not there yet
    // (a little lower still, and transparent), 1 = resting under the top page
    val underIn = remember { Animatable(1f) }

    SceneColumn {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .graphicsLayer {
                    val t = pageIn.value
                    alpha = t.coerceIn(0f, 1f)
                    translationY = (1f - t) * 40.dp.toPx()
                }
        ) {
            // The blank page beneath: tucked a little lower and smaller, rising to take the top
            // page's place as it lifts away
            PaperPage(
                modifier = Modifier.graphicsLayer {
                    val p = lift.value
                    val settle = 1f - underIn.value
                    val scale = UNDER_SCALE + (1f - UNDER_SCALE) * p - 0.03f * settle
                    scaleX = scale
                    scaleY = scale
                    translationY = ((1f - p) + settle) * UNDER_DROP.toPx()
                    alpha = (UNDER_ALPHA + (1f - UNDER_ALPHA) * p) * underIn.value
                }
            ) {}

            // The page with the line on it. Tapped, it lifts off to the left with a slight tilt
            // and fades; once it's gone the next opening is written onto the risen page.
            PaperPage(
                modifier = Modifier
                    .graphicsLayer {
                        val p = lift.value
                        translationX = -p * size.width * 0.45f
                        translationY = -p * 28.dp.toPx()
                        rotationZ = -p * 7f
                        val scale = 1f + 0.03f * p
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - p
                    }
                    // No ripple: the page that lifts away snaps back as the next one, and a
                    // ripple still fading on it would flash across the fresh page.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (lift.isRunning || underIn.isRunning) return@clickable
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        scope.launch {
                            lift.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
                            // The risen page now looks exactly like the top one at rest, blank:
                            // swap them back in the same frame and start writing the next line.
                            index = (index + 1) % Openings.size
                            // The next page beneath starts out of sight, then eases up into place
                            underIn.snapTo(0f)
                            lift.snapTo(0f)
                            underIn.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                        }
                    }
            ) {
                WrittenLine(opening = Openings[index], active = active, key = index)
            }
        }
        Spacer(Modifier.height(20.dp))
        Pill("Tap the page for the next one", Icons.Default.TouchApp, Modifier.rise({ hintIn.value }, 12.dp))
        Spacer(Modifier.height(44.dp))
        SceneText(
            title = "Every story starts\nwith a first line",
            body = "Quill is a calm place to read: your own books and thousands of free classics, with a companion who reads along.",
            active = active,
            delayMs = 1400
        )
    }
}

/** How the page beneath sits while the top one is at rest: a little smaller, lower and fainter. */
private const val UNDER_SCALE = 0.94f
private val UNDER_DROP = 12.dp
private const val UNDER_ALPHA = 0.6f

/** A sheet of the book: paper-coloured, a faint spine shadow down its left edge, [content] inset. */
@Composable
private fun PaperPage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent),
                                endX = 18.dp.toPx()
                            )
                        )
                    }
                }
                .padding(start = 28.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)
        ) {
            content()
        }
    }
}

/** [opening]'s line appearing a word at a time, then who wrote it. Starts over for each new [key]. */
@Composable
private fun WrittenLine(opening: Opening, active: Boolean, key: Int) {
    val colors = MaterialTheme.colorScheme
    val words = remember(opening) { opening.line.split(" ") }
    var shown by remember(key) { mutableIntStateOf(0) }
    // Who wrote it: hidden from a new page's very first frame (not faded out from the last
    // page's), and faded in only once its line is written.
    val sourceIn = remember(key) { Animatable(0f) }
    LaunchedEffect(key, active) {
        if (!active) return@LaunchedEffect
        // The first page waits for the scene to settle; new pages write straight away.
        delay(if (key == 0) 700 else 120)
        for (i in 1..words.size) {
            shown = i
            delay(85)
        }
        sourceIn.animateTo(1f, tween(400))
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = buildAnnotatedString {
                words.forEachIndexed { i, word ->
                    // Words not written yet keep their place, so the line never reflows
                    withStyle(SpanStyle(color = if (i < shown) colors.onSurface else Color.Transparent)) { append(word) }
                    if (i < words.lastIndex) append(" ")
                }
            },
            fontFamily = FontFamily.Serif,
            fontSize = if (opening.line.length > 80) 19.sp else 24.sp,
            lineHeight = if (opening.line.length > 80) 28.sp else 34.sp
        )
        Text(
            text = "— ${opening.source}",
            style = MaterialTheme.typography.labelLarge,
            fontStyle = FontStyle.Italic,
            color = colors.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { alpha = sourceIn.value }
        )
    }
}

// ---------------------------------------------------------------- 2. The library

private class Book(val title: String, val author: String, val width: Dp, val height: Dp)

private val Shelf = listOf(
    Book("Pride and Prejudice", "Jane Austen", 44.dp, 176.dp),
    Book("Moby-Dick", "Herman Melville", 50.dp, 196.dp),
    Book("Frankenstein", "Mary Shelley", 38.dp, 160.dp),
    Book("Dracula", "Bram Stoker", 46.dp, 184.dp),
    Book("Little Women", "Louisa May Alcott", 42.dp, 168.dp),
    Book("The Odyssey", "Homer", 48.dp, 190.dp)
)

/**
 * A shelf of classics: the books slide in one by one and land upright, spines out. Tap one and it
 * lifts off the shelf, with its title and author under the shelf.
 */
@Composable
internal fun LibraryScene(active: Boolean) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var picked by remember { mutableStateOf<Int?>(null) }
    val shelfIn = rememberEntrance(active, 200)
    val covers = listOf(
        colors.primary to colors.onPrimary,
        colors.tertiary to colors.onTertiary,
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.secondary to colors.onSecondary,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        colors.surface to colors.onSurface
    )

    SceneColumn {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(216.dp)
            ) {
                Shelf.forEachIndexed { i, book ->
                    val (cover, ink) = covers[i % covers.size]
                    BookSpine(
                        book = book,
                        cover = cover,
                        ink = ink,
                        lifted = picked == i,
                        entrance = rememberEntrance(active, 350 + i * 130),
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            picked = if (picked == i) null else i
                        }
                    )
                }
            }
            // The shelf they stand on
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(8.dp)
                    .graphicsLayer {
                        scaleX = shelfIn.value.coerceIn(0f, 1f)
                        alpha = shelfIn.value.coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.onBackground.copy(alpha = 0.45f))
            )
            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = picked,
                    transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(120))) },
                    label = "pickedBook"
                ) { index ->
                    if (index == null) {
                        Text(
                            text = "Tap a book",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onBackground.copy(alpha = 0.8f)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Shelf[index].title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(Shelf[index].author, style = MaterialTheme.typography.labelMedium, color = colors.onBackground.copy(alpha = 0.75f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        SceneText(
            title = "A library that's yours",
            body = "Import your own EPUBs or download free classics from Project Gutenberg and Standard Ebooks, then sort them into your own shelves.",
            active = active,
            delayMs = 1100
        )
    }
}

@Composable
private fun BookSpine(book: Book, cover: Color, ink: Color, lifted: Boolean, entrance: Animatable<Float, *>, onTap: () -> Unit) {
    val measurer = rememberTextMeasurer()
    val lift by animateFloatAsState(if (lifted) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "bookLift")
    val titleStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    Box(
        modifier = Modifier
            .size(width = book.width, height = book.height)
            .graphicsLayer {
                val t = entrance.value
                alpha = t.coerceIn(0f, 1f)
                // Slides in from the right and drops into place
                translationX = (1f - t) * 120.dp.toPx()
                translationY = -lift * 18.dp.toPx()
                rotationZ = (1f - t) * 8f
            }
            .pressClickable(shape = RoundedCornerShape(6.dp), onClick = onTap)
            .drawWithCache {
                // The title runs up the spine, as on a real shelf
                val layout = measurer.measure(
                    text = book.title,
                    style = titleStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = (size.height - 28.dp.toPx()).toInt().coerceAtLeast(1))
                )
                onDrawBehind {
                    drawRoundRect(color = cover, cornerRadius = CornerRadius(6.dp.toPx()))
                    // Two bands near the ends of the spine
                    val band = ink.copy(alpha = 0.35f)
                    drawRect(band, topLeft = Offset(0f, 12.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
                    drawRect(band, topLeft = Offset(0f, size.height - 14.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
                    rotate(-90f) {
                        drawText(
                            textLayoutResult = layout,
                            color = ink,
                            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
                        )
                    }
                }
            }
    )
}

// ---------------------------------------------------------------- 3. The reader

private class PageTheme(val name: String, val paper: Color, val ink: Color)

private val PageThemes = listOf(
    PageTheme("Paper", Color(0xFFFFFDF8), Color(0xFF1F1B16)),
    PageTheme("Sepia", Color(0xFFF3E7D3), Color(0xFF5B4636)),
    PageTheme("Night", Color(0xFF161514), Color(0xFFE6E1DA))
)

private const val PASSAGE_START = "It is a truth universally acknowledged, that "
private const val PASSAGE_HIGHLIGHT = "a single man in possession of a good fortune"
private const val PASSAGE_END = ", must be in want of a wife."

/**
 * A page of the reader, live: switch its theme and text size and watch it change, while a line
 * gets highlighted as a reader would.
 */
@Composable
internal fun ReaderScene(active: Boolean) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val pageIn = rememberEntrance(active, 150)
    val controlsIn = rememberEntrance(active, 450)
    var theme by remember { mutableIntStateOf(0) }
    var large by remember { mutableStateOf(false) }
    val highlight = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        delay(1200)
        highlight.animateTo(1f, tween(700))
    }
    val paper by animateColorAsState(PageThemes[theme].paper, tween(400), label = "readerPaper")
    val ink by animateColorAsState(PageThemes[theme].ink, tween(400), label = "readerInk")
    val textSize by animateFloatAsState(if (large) 23f else 18f, spring(dampingRatio = 0.8f), label = "readerSize")
    val marker = colors.tertiary

    SceneColumn {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = paper,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                // At least this tall, and otherwise as tall as its text. The text size itself
                // animates, so the page grows with it frame by frame, its rounded corners intact.
                // (animateContentSize here would clip the page square while it caught up.)
                .heightIn(min = 200.dp)
                .rise({ pageIn.value }, 36.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append(PASSAGE_START)
                    withStyle(SpanStyle(background = marker.copy(alpha = 0.35f * highlight.value))) { append(PASSAGE_HIGHLIGHT) }
                    append(PASSAGE_END)
                },
                color = ink,
                fontFamily = FontFamily.Serif,
                fontSize = textSize.sp,
                lineHeight = (textSize * 1.5f).sp,
                modifier = Modifier.padding(24.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.rise({ controlsIn.value }, 16.dp)
        ) {
            PageThemes.forEachIndexed { i, pageTheme ->
                ThemeSwatch(pageTheme, selected = theme == i) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    theme = i
                }
            }
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (large) colors.primary else colors.surfaceContainerHigh,
                contentColor = if (large) colors.onPrimary else colors.onSurface,
                modifier = Modifier
                    .size(48.dp)
                    .pressClickable(shape = CircleShape) {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        large = !large
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Aa", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
        Spacer(Modifier.height(36.dp))
        SceneText(
            title = "Read it your way",
            body = "Fonts, spacing, page themes, ambient sounds and a warm night light. Highlight the lines that stay with you.",
            active = active,
            delayMs = 700
        )
    }
}

@Composable
private fun ThemeSwatch(theme: PageTheme, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .pressClickable(shape = CircleShape, onClick = onClick)
            .clip(CircleShape)
            .background(theme.paper)
            .border(if (selected) 3.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, CircleShape)
    ) {
        Text("A", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = theme.ink)
    }
}

// ---------------------------------------------------------------- 4. Aira

private const val DEMO_QUESTION = "Who is Mr. Darcy so far?"
private const val DEMO_REPLY = "Mr. Bingley's proud, wealthy friend. At the Meryton ball he calls Elizabeth only " +
    "\"tolerable\", and she hasn't forgiven him yet."

/** The reply appears a few words at a time: this many words per step, this far apart, each fading in over [CHUNK_FADE_MS]. */
private const val WORDS_PER_CHUNK = 3
private const val CHUNK_STEP_MS = 110
private const val CHUNK_FADE_MS = 250

/**
 * Aira, still in her circle, answering a question the way Hajime's Sensei does in its onboarding:
 * the reader's bubble arrives, the thinking dots pulse, then the reply grows across the full
 * width a few words at a time, each group fading in.
 */
@Composable
internal fun AiraScene(active: Boolean) {
    val colors = MaterialTheme.colorScheme
    val portraitIn = rememberEntrance(active, 100)
    val lockIn = rememberEntrance(active, 350)
    val ruleIn = rememberEntrance(active, 250)
    var asked by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        delay(600)
        asked = true
        delay(500)
        thinking = true
        delay(1100)
        thinking = false
        answered = true
    }

    SceneColumn {
        // Aira's circle from the chat's empty state, at 4/5 of its size. She stays still: she only
        // fades in, so the eye goes to the conversation.
        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer { alpha = portraitIn.value.coerceIn(0f, 1f) }
                .border(width = 1.dp, color = colors.primary, shape = CircleShape)
                .clip(CircleShape)
                .background(colors.surface)
                .background(colors.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Image(
                painter = painterResource(R.drawable.psmile_calm),
                contentDescription = "Aira",
                modifier = Modifier
                    .requiredSize(144.dp)
                    .offset(x = (-1.6).dp, y = 12.8.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        // How far into the book she's allowed to talk about
        Pill("Spoiler lock · up to Chapter 3", Icons.Default.Lock, Modifier.rise({ lockIn.value }, 10.dp))
        Spacer(Modifier.height(16.dp))

        // The chat: 12dp between messages, the reader's bubble fading in
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = asked, enter = fadeIn(tween(250))) {
                DemoQuestionBubble(DEMO_QUESTION)
            }
            AnimatedVisibility(visible = thinking, enter = fadeIn(tween(250)), exit = ExitTransition.None) {
                DemoThinkingDots()
            }
            if (answered) DemoReply(DEMO_REPLY)
        }

        Spacer(Modifier.height(16.dp))
        // A quiet rule setting the conversation apart from the words about it
        SceneRule(Modifier.graphicsLayer { alpha = ruleIn.value.coerceIn(0f, 1f) })
        Spacer(Modifier.height(20.dp))
        SceneText(
            title = "Meet Aira",
            body = "Your reading companion answers from the book itself, and never spoils what's ahead.",
            active = active,
            delayMs = 300
        )
    }
}

/** The reader's question: a primary-container bubble on the right, squared at the top right, at most 78% wide. */
@Composable
private fun DemoQuestionBubble(text: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * 0.78f
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                // The primary: the chat's primary-container bubble would vanish into the aura
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.widthIn(max = bubbleMaxWidth)
            ) {
                TightText(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/** Three primary dots pulsing between faint and full, each 200ms behind the one before. */
@Composable
private fun DemoThinkingDots() {
    val transition = rememberInfiniteTransition(label = "thinking")
    Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 200)
                    ),
                    label = "dot$index"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                )
            }
        }
    }
}

/**
 * Aira's reply across the full width, no bubble: it grows a few words at a time, each group
 * fading in, so it builds line by line as if arriving.
 */
@Composable
private fun DemoReply(text: String) {
    val color = MaterialTheme.colorScheme.onBackground
    val chunks = remember(text) { text.split(" ").chunked(WORDS_PER_CHUNK).map { it.joinToString(" ") } }
    val total = (chunks.size - 1) * CHUNK_STEP_MS + CHUNK_FADE_MS
    val elapsed = remember(text) { Animatable(0f) }
    LaunchedEffect(text) { elapsed.animateTo(total.toFloat(), tween(total, easing = LinearEasing)) }

    val now = elapsed.value
    Text(
        text = buildAnnotatedString {
            chunks.forEachIndexed { i, chunk ->
                val since = now - i * CHUNK_STEP_MS
                // Only the groups that have started are laid out, so the reply grows downwards
                if (since < 0f) return@forEachIndexed
                val alpha = (since / CHUNK_FADE_MS).coerceIn(0f, 1f)
                withStyle(SpanStyle(color = color.copy(alpha = alpha))) {
                    append(if (i == 0) chunk else " $chunk")
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

/**
 * A hairline across the scene that fades out at both ends, with Aira's star (the ✦ from her
 * chat's message field) at its centre: a quiet break between the conversation and the words about it.
 */
@Composable
private fun SceneRule(modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, line)))
        )
        // The star from Aira's message field in her chat
        Text(
            text = "✦",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(line, Color.Transparent)))
        )
    }
}

// ---------------------------------------------------------------- 5. The habit

private const val HEAT_WEEKS = 12
private const val STREAK_DAYS = 12

/**
 * A reading heatmap filling in week by week, darker for longer reading days, then a streak
 * counting up.
 */
@Composable
internal fun HabitScene(active: Boolean) {
    // The card is in the app's own colours: its heatmap and streak use the accent colour
    val colors = paperColors()
    val haptic = LocalHapticFeedback.current
    val cardIn = rememberEntrance(active, 300)
    val fill = remember { Animatable(0f) }
    var streak by remember { mutableIntStateOf(0) }
    // How much was read each day: mostly something, more toward the recent weeks.
    val days = remember {
        val random = Random(11)
        List(HEAT_WEEKS * 7) { i ->
            val recent = i / (HEAT_WEEKS * 7f)
            if (random.nextFloat() < 0.25f - recent * 0.2f) 0f else (0.25f + random.nextFloat() * 0.5f + recent * 0.25f).coerceAtMost(1f)
        }
    }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        delay(600)
        fill.animateTo(HEAT_WEEKS.toFloat(), tween(1500, easing = FastOutSlowInEasing))
        for (day in 1..STREAK_DAYS) {
            streak = day
            if (day % 3 == 0) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            delay(55)
        }
    }

    SceneColumn {
        SceneText(
            title = "Make reading a habit",
            body = "Streaks, a reading heatmap and gentle daily reminders keep the pages turning.",
            active = active,
            delayMs = 100
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .rise({ cardIn.value }, 28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(HEAT_WEEKS) { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { day ->
                                val amount = days[week * 7 + day]
                                // Each week's column lights up as the fill sweeps past it
                                val lit = (fill.value - week).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.onSurfaceVariant.copy(alpha = 0.10f))
                                        .graphicsLayer { alpha = lit }
                                        .background(colors.primary.copy(alpha = amount))
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$streak",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = colors.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}
