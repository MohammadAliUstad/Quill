package com.yugentech.quill.ui.dash.screens.readerScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.dash.common.itemShape
import org.readium.r2.shared.publication.Link

private data class TocDisplayItem(
    val link: Link,
    val depth: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTableOfContents(
    toc: List<Link>,
    onTocItemClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val flattenedToc = remember(toc) { flattenToc(toc) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(flattenedToc) { index, item ->
                    TocItemRow(
                        item = item,
                        index = index,
                        allItems = flattenedToc,
                        onClick = onTocItemClick
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

private fun flattenToc(links: List<Link>, depth: Int = 0): List<TocDisplayItem> {
    val result = mutableListOf<TocDisplayItem>()
    for (link in links) {
        result.add(TocDisplayItem(link, depth))
        if (link.children.isNotEmpty()) {
            result.addAll(flattenToc(link.children, depth + 1))
        }
    }
    return result
}

@Composable
private fun TocItemRow(
    item: TocDisplayItem,
    index: Int,
    allItems: List<TocDisplayItem>,
    onClick: (String) -> Unit
) {
    val topPadding = calculateTopPadding(item.depth, index, allItems)
    val styling = getChapterStyling(item.depth)

    if (topPadding > 0.dp) {
        Spacer(modifier = Modifier.height(topPadding))
    }

    if (item.depth >= 2) {
        RegularTocItem(item, index, allItems, styling, onClick)
    } else {
        HeaderTocItem(item, styling, onClick)
    }
}

@Composable
private fun RegularTocItem(
    item: TocDisplayItem,
    index: Int,
    allItems: List<TocDisplayItem>,
    styling: ChapterStyling,
    onClick: (String) -> Unit
) {
    // Determine shape based on grouping
    val depth = item.depth
    val sequenceStart = (index downTo 0).takeWhile { allItems[it].depth == depth }.last()
    val sequenceEnd = (index..allItems.lastIndex).takeWhile { allItems[it].depth == depth }.last()
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
                .clickable { onClick(item.link.href.toString()) }
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
                text = item.link.title ?: "Untitled",
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
private fun HeaderTocItem(
    item: TocDisplayItem,
    styling: ChapterStyling,
    onClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = if (item.depth == 0) CircleShape else RoundedCornerShape(12.dp),
        color = styling.backgroundColor,
        tonalElevation = if (item.depth == 0) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item.link.href.toString()) }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (item.depth == 0) Arrangement.Center else Arrangement.Start
        ) {
            Text(
                text = item.link.title ?: "Untitled",
                style = styling.textStyle,
                color = styling.textColor,
                fontWeight = styling.fontWeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Styling Helpers (Ported from ChaptersListSection) ---

private fun calculateTopPadding(depth: Int, index: Int, items: List<TocDisplayItem>): Dp {
    if (index == 0) return 0.dp
    val prevDepth = items.getOrNull(index - 1)?.depth
    val isGroupStart = prevDepth != depth
    return when {
        depth == 0 -> 2.dp
        depth == 1 -> 2.dp
        isGroupStart -> 2.dp
        else -> 0.dp
    }
}

private data class ChapterStyling(
    val textStyle: TextStyle,
    val fontWeight: FontWeight,
    val textColor: Color,
    val backgroundColor: Color
)

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
            0 -> MaterialTheme.colorScheme.onPrimaryContainer
            1 -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        backgroundColor = when (depth) {
            0 -> MaterialTheme.colorScheme.primaryContainer
            1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        }
    )
}