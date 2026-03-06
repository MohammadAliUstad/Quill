package com.yugentech.quill.ui.shared.bookDetailsScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Chapter
import com.yugentech.quill.ui.mainScreen.components.itemShape

@Composable
fun ChaptersListSection(
    chapters: List<Chapter>,
    onChapterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Chapters",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            chapters.forEachIndexed { index, chapter ->
                ChapterItem(
                    chapter = chapter,
                    index = index,
                    chapters = chapters,
                    onClick = onChapterClick
                )
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: Chapter,
    index: Int,
    chapters: List<Chapter>,
    onClick: (String) -> Unit
) {
    val depth = chapter.depth
    val topPadding = calculateTopPadding(depth, index, chapters)
    val styling = getChapterStyling(depth)

    if (topPadding > 0.dp) {
        Spacer(modifier = Modifier.height(topPadding))
    }

    if (depth >= 2) {
        RegularChapterItem(chapter, index, chapters, styling, onClick)
    } else {
        HeaderChapterItem(chapter, styling, onClick)
    }
}

@Composable
private fun RegularChapterItem(
    chapter: Chapter,
    index: Int,
    chapters: List<Chapter>,
    styling: ChapterStyling,
    onClick: (String) -> Unit
) {
    val depth = chapter.depth

    val sequenceStart = (index downTo 0).takeWhile { chapters[it].depth == depth }.last()
    val sequenceEnd = (index..chapters.lastIndex).takeWhile { chapters[it].depth == depth }.last()
    val sequenceIndex = index - sequenceStart
    val sequenceCount = sequenceEnd - sequenceStart + 1

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = itemShape(sequenceIndex, sequenceCount),
        color = styling.backgroundColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onClick(chapter.href) })
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = chapter.title,
                style = styling.textStyle,
                color = styling.textColor,
                fontWeight = styling.fontWeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeaderChapterItem(
    chapter: Chapter,
    styling: ChapterStyling,
    onClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = if (chapter.depth == 0) CircleShape else RoundedCornerShape(12.dp),
        color = styling.backgroundColor,
        tonalElevation = if (chapter.depth == 0) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onClick(chapter.href) })
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (chapter.depth == 0) Arrangement.Center else Arrangement.Start
        ) {
            Text(
                text = chapter.title,
                style = styling.textStyle,
                color = styling.textColor,
                fontWeight = styling.fontWeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun calculateTopPadding(depth: Int, index: Int, chapters: List<Chapter>): Dp {
    if (index == 0) return 0.dp
    val prevDepth = chapters.getOrNull(index - 1)?.depth
    val isGroupStart = prevDepth != depth
    return when {
        depth == 0 -> 2.dp
        depth == 1 -> 2.dp
        isGroupStart -> 2.dp
        else -> 0.dp
    }
}

@Composable
private fun getChapterStyling(depth: Int): ChapterStyling {
    return ChapterStyling(
        textStyle = when (depth) {
            0 -> MaterialTheme.typography.titleLarge
            1 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.bodyLarge
        },
        fontWeight = when (depth) {
            0 -> FontWeight.ExtraBold
            1 -> FontWeight.Bold
            else -> FontWeight.Medium
        },
        textColor = when (depth) {
            0 -> MaterialTheme.colorScheme.onSecondaryContainer
            1 -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        backgroundColor = when (depth) {
            0 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        }
    )
}

private data class ChapterStyling(
    val textStyle: TextStyle,
    val fontWeight: FontWeight,
    val textColor: Color,
    val backgroundColor: Color
)