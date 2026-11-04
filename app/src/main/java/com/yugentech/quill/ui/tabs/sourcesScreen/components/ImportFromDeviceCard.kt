package com.yugentech.quill.ui.tabs.sourcesScreen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportFromDeviceCard(
    isImporting: Boolean,
    importedCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cornerRadius = 28.dp
    // Two characters sit on the card's top edge: the reader from `growth` on the right, and the
    // girl hugging a puppy from `puppy` beside them, facing the reader. The card starts
    // `overhang` down; each drawing is pushed down so its feet land ~10dp inside the card.
    // Feet sit ~94% down the `growth` image and ~89% down the `puppy` one.
    val overhang = 132.dp
    val readerSize = 150.dp
    val puppySize = 132.dp
    val puppyTop = overhang + 10.dp - puppySize * 0.89f
    // Puts the puppy drawing's right edge (it has ~23% blank on that side) just left of the reader.
    val puppyEnd = 126.dp

    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Your files",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = overhang - 28.dp),
        )

        ImportCardSurface(
            isImporting = isImporting,
            importedCount = importedCount,
            cornerRadius = cornerRadius,
            onClick = onClick,
            modifier = Modifier.padding(top = overhang),
        )

        Image(
            painter = painterResource(id = R.drawable.puppy),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = puppyEnd, top = puppyTop)
                .size(puppySize),
        )

        Image(
            painter = painterResource(id = R.drawable.growth),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp)
                .size(readerSize),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportCardSurface(
    isImporting: Boolean,
    importedCount: Int,
    cornerRadius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = !isImporting,
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                cornerRadius = cornerRadius
            ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialShapes.Bun.toShape(),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isImporting) "Importing..." else "Import from Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (importedCount > 0) {
                        "EPUB files · $importedCount imported"
                    } else {
                        "Bring your own EPUB files"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.tertiary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Browse Device",
                                tint = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// Drawn after the content so the Surface's own background doesn't cover it.
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp,
): Modifier = drawWithContent {
    drawContent()
    val stroke = strokeWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx() - stroke / 2),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
        )
    )
}
