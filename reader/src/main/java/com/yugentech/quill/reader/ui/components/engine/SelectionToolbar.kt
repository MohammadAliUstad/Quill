package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
                .padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAiraReady) {
                ToolbarAction(
                    modifier = Modifier.weight(1f),
                    icon = null,
                    label = "Ask Aira",
                    tint = primaryColor,
                    customContent = { AiraBadge() },
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

// Aira's mark: the same Cookie9Sided shape used on the Aira screen, turning slowly so the
// action reads as "live" without the glow fighting the sparkle for contrast. Only the
// shape rotates; the sparkle stays upright.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AiraBadge() {
    val rotation by rememberInfiniteTransition(label = "aira_badge").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(IconSlotSize)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation }
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialShapes.Cookie9Sided.toShape()
                )
        )
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = "Ask Aira",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
        )
    }
}

// Every action gets the same icon slot so the labels line up across the row.
private val IconSlotSize = 32.dp

@Composable
private fun ToolbarAction(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    customContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(IconSlotSize)
        ) {
            if (customContent != null) {
                customContent()
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                    tint = tint
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            ),
            color = if (customContent != null) tint else tint.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}
