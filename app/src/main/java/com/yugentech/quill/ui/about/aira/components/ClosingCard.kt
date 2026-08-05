package com.yugentech.quill.ui.about.aira.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val visionPoints = listOf(
    "Books come alive with context, not spoilers.",
    "Every question answered from the page itself.",
    "A reading companion that grows with you."
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ClosingCard() {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(primaryContainer)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon badge
            Surface(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = primary.copy(alpha = 0.18f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Manifesto headline
            Text(
                text = "Built for the\nreaders who\nnever stop.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = onPrimaryContainer,
                lineHeight = 40.sp
            )

            HorizontalDivider(
                color = onPrimaryContainer.copy(alpha = 0.18f),
                thickness = 0.5.dp
            )

            // Vision points
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                visionPoints.forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(onPrimaryContainer.copy(alpha = 0.45f))
                        )
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onPrimaryContainer.copy(alpha = 0.78f),
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}
