package com.yugentech.quill.ui.more.aboutAira.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrinciplesSection() {
    val principles = listOf(
        PrincipleItem(
            icon = Icons.Outlined.FindInPage,
            title = "Grounded in the Text",
            body = "Aira doesn't guess. Her answers are strictly retrieved from the actual passages of the book you are reading.",
            shape = MaterialShapes.Bun.toShape()
        ),
        PrincipleItem(
            icon = Icons.Outlined.Psychology,
            title = "Context-Aware",
            body = "She knows exactly which chapter you're on and remembers your ongoing conversation for a natural chat experience.",
            shape = MaterialShapes.Clover8Leaf.toShape()
        ),
        PrincipleItem(
            icon = Icons.Outlined.Shield,
            title = "Protects the Plot",
            body = "Aira's knowledge is strictly locked to your reading progress. She will never reveal events or twists from chapters you haven't read.",
            shape = MaterialShapes.Slanted.toShape()
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        principles.forEach { item ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Changed to 'primary' to make the shapes highly visible in light mode
                    Surface(
                        shape = item.shape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer, // Adjusted for contrast
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

data class PrincipleItem(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val shape: Shape
)