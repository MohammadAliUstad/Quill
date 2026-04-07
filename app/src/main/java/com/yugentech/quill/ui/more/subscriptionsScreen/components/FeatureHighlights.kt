package com.yugentech.quill.ui.more.subscriptions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FeatureHighlights() {
    val features = listOf(
        FeatureItem(
            title = "Smart Chapter Summaries",
            description = "Get instant recaps of any chapter to keep the plot fresh in your mind.",
            icon = Icons.Default.Description
        ),
        FeatureItem(
            title = "Character & Identity Tracking",
            description = "Identify who's speaking and understand the significance of every character.",
            icon = Icons.Default.Psychology
        ),
        FeatureItem(
            title = "Reading & Linguistic Aid",
            description = "Simplify difficult passages and get instant definitions for unfamiliar words.",
            icon = Icons.Default.Translate
        ),
        FeatureItem(
            title = "Interactive Story Chat",
            description = "Ask Aira anything about the book's themes, plot, or narrative significance.",
            icon = Icons.Default.ChatBubbleOutline
        ),
        FeatureItem(
            title = "Active Spoiler Protection",
            description = "Aira's knowledge is locked to your progress, preventing accidental spoilers.",
            icon = Icons.Default.Lock
        )
    )

    Column(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        features.forEach { feature ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)