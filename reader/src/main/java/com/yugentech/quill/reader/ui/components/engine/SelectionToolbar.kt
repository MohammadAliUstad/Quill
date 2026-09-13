package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush as GradientBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.theme.service.HapticService
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import org.koin.compose.koinInject

@Composable
fun SelectionToolbar(
    selectionInfo: SelectionInfo,
    isAiraReady: Boolean,
    hazeState: HazeState,
    readerBgIsLight: Boolean = false,
    onHighlight: () -> Unit,
    onAskAira: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = koinInject<HapticService>()
    val primaryColor = MaterialTheme.colorScheme.primary

    // On light ebook backgrounds the frosted glass blurs to near-white and disappears.
    // Use a more opaque, slightly darker surface so the toolbar stays readable on any theme.
    val hazeBackgroundColor = if (readerBgIsLight) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(28.dp))
            .hazeEffect(
                state = hazeState,
                style = HazeDefaults.style(
                    backgroundColor = hazeBackgroundColor,
                    blurRadius = 14.dp,
                    noiseFactor = 0.05f
                )
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAiraReady) {
                ToolbarAction(
                    modifier = Modifier.weight(1f),
                    icon = null,
                    label = "Ask Aira",
                    tint = primaryColor,
                    isAnimated = true,
                    customContent = { scale, alpha ->
                        // scale swings 1.0-1.1; stretch that same phase into a
                        // stronger 0.5-1.0 alpha swing so the glow visibly breathes.
                        val glowAlpha = 0.5f + (scale - 1f) * 5f
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = glowAlpha
                                    }
                                    .background(
                                        brush = GradientBrush.radialGradient(
                                            0.0f to primaryColor.copy(alpha = 1f),
                                            0.4f to primaryColor.copy(alpha = 0.55f),
                                            1.0f to Color.Transparent,
                                            radius = with(LocalDensity.current) { 19.dp.toPx() }
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .border(1.5.dp, primaryColor.copy(alpha = 0.4f), CircleShape)
                            )
                            Text(
                                text = "✦",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 18.sp
                                ),
                                color = primaryColor,
                                modifier = Modifier
                                    .offset(y = (-2).dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                            )
                        }
                    },
                    onClick = {
                        haptic.performHaptic()
                        onAskAira(selectionInfo.text)
                    }
                )
            }

            ToolbarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Brush,
                label = "Highlight",
                tint = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    haptic.performHaptic()
                    onHighlight()
                }
            )

            ToolbarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.ContentCopy,
                label = "Copy",
                onClick = {
                    haptic.performHaptic()
                    onCopy(selectionInfo.text)
                }
            )

            ToolbarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Share,
                label = "Share",
                onClick = {
                    haptic.performHaptic()
                    onShare(selectionInfo.text)
                }
            )
        }
    }
}

@Composable
private fun ToolbarAction(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    isAnimated: Boolean = false,
    customContent: (@Composable (scale: Float, alpha: Float) -> Unit)? = null,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
    val scale by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) }
    }

    val alpha by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (customContent != null) {
            customContent(scale, alpha)
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                tint = tint
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            ),
            color = if (isAnimated) tint else tint.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}
