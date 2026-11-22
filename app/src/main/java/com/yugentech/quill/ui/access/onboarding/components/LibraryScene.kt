package com.yugentech.quill.ui.access.onboarding.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.quill.ui.access.common.components.SceneColumn
import com.yugentech.quill.ui.access.common.components.SceneText
import com.yugentech.quill.ui.access.common.components.rememberEntrance
import com.yugentech.quill.ui.common.components.pressClickable

private class ShelfBook(val title: String, val author: String, val width: Dp, val height: Dp)

private val Shelf = listOf(
    ShelfBook("Pride and Prejudice", "Jane Austen", 44.dp, 176.dp),
    ShelfBook("Moby-Dick", "Herman Melville", 50.dp, 196.dp),
    ShelfBook("Frankenstein", "Mary Shelley", 38.dp, 160.dp),
    ShelfBook("Dracula", "Bram Stoker", 46.dp, 184.dp),
    ShelfBook("Little Women", "Louisa May Alcott", 42.dp, 168.dp),
    ShelfBook("The Odyssey", "Homer", 48.dp, 190.dp)
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
                            text = stringResource(R.string.onboarding_library_tap),
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
            title = stringResource(R.string.onboarding_library_title),
            body = stringResource(R.string.onboarding_library_body),
            active = active,
            delayMs = 1100
        )
    }
}

@Composable
private fun BookSpine(book: ShelfBook, cover: Color, ink: Color, lifted: Boolean, entrance: Animatable<Float, *>, onTap: () -> Unit) {
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
                    drawRect(band, topLeft = Offset(0f, 12.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
                    drawRect(band, topLeft = Offset(0f, size.height - 14.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
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
