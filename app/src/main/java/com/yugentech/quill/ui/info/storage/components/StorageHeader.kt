package com.yugentech.quill.ui.info.storage.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun StorageHeader(
    appUsedBytes: Long,
    freeBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    val deviceUsedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
    val deviceUsedPercent =
        if (totalBytes > 0) deviceUsedBytes.toFloat() / totalBytes.toFloat() else 0f

    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimated = true }

    val animatedPercent by animateFloatAsState(
        targetValue = if (isAnimated) deviceUsedPercent else 0f,
        animationSpec = tween(1000),
        label = "deviceUsedPercent"
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Downloads Size",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = formatBytes(appUsedBytes),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )


        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "Device Storage",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { animatedPercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surfaceVariant,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StorageLegendItem(
                label = "Used Space",
                size = formatBytes(deviceUsedBytes),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            StorageLegendItem(
                label = "Free Space",
                size = formatBytes(freeBytes),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }
}

@Composable
private fun StorageLegendItem(label: String, size: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = size,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.1f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}