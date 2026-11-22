package com.yugentech.quill.ui.access.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
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
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@Composable
fun FeatureHighlights() {
    val features = listOf(
        FeatureItem(
            title = stringResource(R.string.feature_summaries_title),
            description = stringResource(R.string.feature_summaries_body),
            icon = Icons.Default.Description
        ),
        FeatureItem(
            title = stringResource(R.string.feature_characters_title),
            description = stringResource(R.string.feature_characters_body),
            icon = Icons.Default.Psychology
        ),
        FeatureItem(
            title = stringResource(R.string.feature_language_title),
            description = stringResource(R.string.feature_language_body),
            icon = Icons.Default.Translate
        ),
        FeatureItem(
            title = stringResource(R.string.feature_chat_title),
            description = stringResource(R.string.feature_chat_body),
            icon = Icons.Default.ChatBubbleOutline
        ),
        FeatureItem(
            title = stringResource(R.string.feature_visualize_title),
            description = stringResource(R.string.feature_visualize_body),
            icon = Icons.Default.Brush
        ),
        FeatureItem(
            title = stringResource(R.string.feature_spoilers_title),
            description = stringResource(R.string.feature_spoilers_body),
            icon = Icons.Default.Lock
        ),
        FeatureItem(
            title = stringResource(R.string.feature_mistakes_title),
            description = stringResource(R.string.feature_mistakes_body),
            icon = Icons.Default.Info
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