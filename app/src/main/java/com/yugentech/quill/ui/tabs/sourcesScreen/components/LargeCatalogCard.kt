package com.yugentech.quill.ui.tabs.sourcesScreen.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.decode.DataSource
import com.yugentech.quill.database.model.BookSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LargeCatalogCard(
    catalog: CatalogInfo,
    libraryCount: Int,
    coverUrls: List<String>,
    modifier: Modifier = Modifier,
    shuffleOffsetMillis: Long = 0L,
    onClick: () -> Unit,
) {
    val contentColor = catalog.contentColor()

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = catalog.containerColor()),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box {
            // Oversized decorative shape bleeding off the top-right corner
            Surface(
                shape = catalog.shape,
                color = contentColor.copy(alpha = 0.06f),
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 70.dp, y = (-60).dp),
            ) {}

            Column(
                modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = catalog.shape,
                        color = contentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = catalog.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = catalog.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = contentColor,
                        )
                        Text(
                            text = catalog.subtitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = contentColor.copy(alpha = 0.8f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = catalog.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.9f),
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    CoverFan(
                        coverUrls = coverUrls,
                        // Solid, not see-through: overlapping placeholders and their shadows
                        // would otherwise show through each other as muddy patches.
                        placeholderColor = lerp(catalog.containerColor(), contentColor, 0.14f),
                        detailColor = contentColor,
                        shuffleOffsetMillis = shuffleOffsetMillis,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        catalog.stats.forEach { stat ->
                            StatChip(
                                text = stat,
                                containerColor = contentColor.copy(alpha = 0.1f),
                                contentColor = contentColor,
                            )
                        }
                        if (libraryCount > 0) {
                            StatChip(
                                text = "$libraryCount in your library",
                                containerColor = catalog.buttonContainerColor(),
                                contentColor = catalog.buttonContentColor(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        shape = CircleShape,
                        color = catalog.buttonContainerColor(),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = catalog.buttonText,
                                tint = catalog.buttonContentColor(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val SHUFFLE_INTERVAL_MILLIS = 3_500L

// Where a cover sits in the fan. x/y are in dp.
private data class FanSlot(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float,
    val alpha: Float,
    val z: Float,
)

private val FrontSlot = FanSlot(x = 0f, y = 0f, rotation = 0f, scale = 1f, alpha = 1f, z = 3f)
private val RightSlot = FanSlot(x = 28f, y = 6f, rotation = 12f, scale = 0.94f, alpha = 1f, z = 2f)
private val LeftSlot = FanSlot(x = -28f, y = 6f, rotation = -12f, scale = 0.94f, alpha = 1f, z = 1f)
private val HiddenSlot = FanSlot(x = 0f, y = 10f, rotation = 0f, scale = 0.8f, alpha = 0f, z = 0f)

// Covers fanned out like a hand of cards. Every few seconds the stack shuffles: the right cover
// swings to the front, the front one tucks to the left, the left one slips away behind, and the
// next cover in line fans out on the right. Until three covers exist, blank covers hold the shape
// (and nothing shuffles) so the card doesn't change size when the feed arrives.
@Composable
private fun CoverFan(
    coverUrls: List<String>,
    placeholderColor: Color,
    detailColor: Color,
    shuffleOffsetMillis: Long,
) {
    val canShuffle = coverUrls.size >= 3
    val covers: List<String?> = if (canShuffle) coverUrls else List(3) { coverUrls.getOrNull(it) }
    val count = covers.size

    var shift by remember(coverUrls) { mutableIntStateOf(0) }

    LaunchedEffect(coverUrls) {
        if (!canShuffle) return@LaunchedEffect
        delay(SHUFFLE_INTERVAL_MILLIS + shuffleOffsetMillis)
        while (true) {
            shift++
            delay(SHUFFLE_INTERVAL_MILLIS)
        }
    }

    Box(
        modifier = Modifier.size(width = 112.dp, height = 100.dp),
        contentAlignment = Alignment.Center,
    ) {
        covers.forEachIndexed { index, url ->
            val slot = when ((index - shift).mod(count)) {
                0 -> FrontSlot
                1 -> RightSlot
                count - 1 -> LeftSlot
                else -> HiddenSlot
            }

            key(url ?: "placeholder-$index") {
                FanCover(
                    url = url,
                    slot = slot,
                    placeholderColor = placeholderColor,
                    detailColor = detailColor,
                )
            }
        }
    }
}

@Composable
private fun FanCover(
    url: String?,
    slot: FanSlot,
    placeholderColor: Color,
    detailColor: Color,
) {
    val coverShape = RoundedCornerShape(6.dp)
    val motion = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)

    val x by animateFloatAsState(slot.x, motion, label = "fanX")
    val y by animateFloatAsState(slot.y, motion, label = "fanY")
    val rotation by animateFloatAsState(slot.rotation, motion, label = "fanRotation")
    val scale by animateFloatAsState(slot.scale, motion, label = "fanScale")
    val alpha by animateFloatAsState(slot.alpha, motion, label = "fanAlpha")

    Box(
        modifier = Modifier
            .zIndex(slot.z)
            .graphicsLayer {
                translationX = x.dp.toPx()
                translationY = y.dp.toPx()
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                shadowElevation = 6.dp.toPx() * alpha
                shape = coverShape
                clip = true
            }
            .size(width = 56.dp, height = 84.dp)
            .background(placeholderColor)
            // A blank cover until the real one fades in over it
            .drawBehind { drawBlankCover(detailColor) }
    ) {
        if (url != null) {
            // The cover fades in over its placeholder once it arrives. One already in memory
            // (back on the screen, say) is shown straight away, since there was no wait to cover.
            val coverAlpha = remember(url) { Animatable(0f) }
            val scope = rememberCoroutineScope()
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { state ->
                    if (state.result.dataSource == DataSource.MEMORY_CACHE) {
                        scope.launch { coverAlpha.snapTo(1f) }
                    } else {
                        scope.launch { coverAlpha.animateTo(1f, tween(durationMillis = 450, easing = LinearOutSlowInEasing)) }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    // this.alpha: the layer's own, not the fan's alpha declared above.
                    .graphicsLayer { this.alpha = coverAlpha.value },
            )
        }
    }
}

/**
 * A cover with nothing printed on it yet: a shaded spine and bars where the title and author go.
 */
private fun DrawScope.drawBlankCover(detail: Color) {
    val spine = size.width * 0.10f
    drawRect(detail.copy(alpha = 0.10f), size = Size(spine, size.height))

    val bar = 4.dp.toPx()
    val left = spine + size.width * 0.14f
    val round = CornerRadius(bar / 2)
    drawRoundRect(
        detail.copy(alpha = 0.22f),
        topLeft = Offset(left, size.height * 0.22f),
        size = Size(size.width * 0.56f, bar),
        cornerRadius = round,
    )
    drawRoundRect(
        detail.copy(alpha = 0.16f),
        topLeft = Offset(left, size.height * 0.22f + bar * 2),
        size = Size(size.width * 0.38f, bar),
        cornerRadius = round,
    )
    drawRoundRect(
        detail.copy(alpha = 0.14f),
        topLeft = Offset(left, size.height * 0.78f),
        size = Size(size.width * 0.44f, bar * 0.8f),
        cornerRadius = round,
    )
}

@Composable
private fun StatChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    // Fixed height + centred box so every chip lines up regardless of font padding.
    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.height(30.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

data class CatalogInfo(
    val source: BookSource,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val shape: Shape,
    val stats: List<String>,
    val containerColor: @Composable () -> Color,
    val contentColor: @Composable () -> Color,
    val buttonContainerColor: @Composable () -> Color,
    val buttonContentColor: @Composable () -> Color,
    val buttonText: String,
)
