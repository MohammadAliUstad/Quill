package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.InsightsReport

/**
 * Total time, streak and books finished as one grouped row: round outer corners, small ones
 * where they meet, each in its own container colour with its icon on an expressive shape.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlanceRow(report: InsightsReport, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        GlanceTile(
            icon = Icons.Outlined.Schedule,
            badge = MaterialShapes.Cookie6Sided.toShape(),
            value = durationString(report.totalReadMillis),
            label = stringResource(R.string.insights_glance_time),
            container = colors.secondaryContainer,
            content = colors.onSecondaryContainer,
            shape = rowItemShape(0, 3),
            modifier = Modifier.weight(1f)
        )
        GlanceTile(
            icon = Icons.Outlined.LocalFireDepartment,
            badge = MaterialShapes.Sunny.toShape(),
            value = "${report.streak}",
            label = stringResource(R.string.insights_glance_streak),
            container = colors.tertiaryContainer,
            content = colors.onTertiaryContainer,
            shape = rowItemShape(1, 3),
            modifier = Modifier.weight(1f)
        )
        GlanceTile(
            icon = Icons.Outlined.CheckCircle,
            badge = MaterialShapes.Clover4Leaf.toShape(),
            value = "${report.finishedBooks}",
            label = stringResource(R.string.insights_glance_finished),
            container = colors.surfaceContainerHigh,
            content = colors.onSurface,
            shape = rowItemShape(2, 3),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GlanceTile(
    icon: ImageVector,
    badge: Shape,
    value: String,
    label: String,
    container: Color,
    content: Color,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(shape = shape, color = container, contentColor = content, modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShapeBadge(icon, badge, content.copy(alpha = 0.12f), content, size = 36.dp)
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
