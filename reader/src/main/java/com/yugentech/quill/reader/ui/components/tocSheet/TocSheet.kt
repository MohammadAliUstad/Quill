package com.yugentech.quill.reader.ui.tocSheet

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.readium.r2.shared.publication.Link

private data class TocDisplayItem(
    val link: Link,
    val depth: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocSheet(
    toc: List<Link>,
    currentHref: String?, // <-- ADDED: To track the active chapter
    onTocItemClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val flattenedToc = remember(toc) { flattenToc(toc) }

    // Find the index of the currently active chapter
    val activeIndex = remember(flattenedToc, currentHref) {
        flattenedToc.indexOfFirst { item ->
            // Match the base href (ignoring anchors/fragments like #id)
            item.link.href.toString().substringBefore("#") == currentHref?.substringBefore("#")
        }
    }

    val sheetState = rememberModalBottomSheetState()

    // Start scrolling at the active index. We add +1 because the "Table of Contents" title is item 0.
    val initialScrollIndex = if (activeIndex >= 0) activeIndex + 1 else 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)

    val cornerRadius by animateDpAsState(
        targetValue = if (sheetState.targetValue == SheetValue.Expanded) 0.dp else 28.dp,
        label = "sheetCornerRadius"
    )

    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius
            ),
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume the click silently */ }
                        )
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        text = "Table of Contents",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                    )
                }

                itemsIndexed(flattenedToc) { index, item ->
                    val isActive = index == activeIndex
                    SimpleTocItem(
                        item = item,
                        isActive = isActive,
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
private fun SimpleTocItem(
    item: TocDisplayItem,
    isActive: Boolean,
    onClick: (String) -> Unit
) {
    // Dynamic styling based on whether this is the currently active chapter
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val textWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp) // Outer padding for separation
            .clip(CircleShape) // Distinct list item shape
            .background(containerColor)
            .clickable { onClick(item.link.href.toString()) }
            .padding(horizontal = 16.dp, vertical = 14.dp), // Inner padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.depth > 0) {
            Spacer(modifier = Modifier.width((item.depth * 16).dp))
        }

        Text(
            text = item.link.title ?: "Untitled",
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = textWeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Add a dot indicator to the active chapter
        if (isActive) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}