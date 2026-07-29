package com.yugentech.quill.reader.ui.components.tocSheet

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.sp
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

private data class TocDisplayItem(
    val link: Link,
    val depth: Int,
    val pageNumber: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocSheet(
    publication: Publication,
    allPositions: List<Locator>,
    currentHref: String?,
    onTocItemClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val toc = publication.tableOfContents
    val flattenedToc = remember(toc, allPositions) { flattenToc(publication, allPositions, toc) }

    val activeIndex = remember(flattenedToc, currentHref) {
        flattenedToc.indexOfFirst { item ->
            item.link.href.toString().substringBefore("#") == currentHref?.substringBefore("#")
        }
    }

    val sheetState = rememberModalBottomSheetState()
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
                            onClick = {}
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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

private fun flattenToc(
    publication: Publication,
    allPositions: List<Locator>,
    links: List<Link>,
    depth: Int = 0
): List<TocDisplayItem> {
    val result = mutableListOf<TocDisplayItem>()
    for (link in links) {
        val locator = publication.locatorFromLink(link)
        val pageNumber = locator?.let { loc ->
            // Try to find the exact position from allPositions if available
            allPositions.firstOrNull { it.href == loc.href }?.locations?.position
                ?: loc.locations.position
        }

        result.add(TocDisplayItem(link, depth, pageNumber?.toString()))
        if (link.children.isNotEmpty()) {
            result.addAll(flattenToc(publication, allPositions, link.children, depth + 1))
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
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val textWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable { onClick(item.link.href.toString()) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
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

        if (item.pageNumber != null) {
            Text(
                text = "Page ${item.pageNumber}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = contentColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

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