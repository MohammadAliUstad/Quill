package com.yugentech.quill.reader.ui.components.aira.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun selectionLabel(text: String): String {
    val wordCount = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    val sentenceCount = text.trim().split(Regex("[.!?]+\\s+")).filter { it.isNotBlank() }.size
    return when {
        wordCount == 1 -> "Selected word"
        wordCount <= 5 -> "Selected words"
        sentenceCount > 1 -> "Selected passage"
        else -> "Selected sentence"
    }
}

@Composable
fun SelectionSnippet(
    text: String,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val displayText = text
        .replace("\\n", " ")
        .replace("\\r", " ")
        .replace("\\t", " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(primary.copy(alpha = 0.06f))
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    color = primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = selectionLabel(displayText),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                ),
                color = primary.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 19.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
