package com.yugentech.quill.ui.config.category.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Category
import com.yugentech.quill.ui.main.components.itemShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun DragDropList(
    modifier: Modifier = Modifier,
    items: List<Category>,
    onReorderFinished: (List<Category>) -> Unit,
    onDelete: (Category) -> Unit,
    onRename: (Category) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val localList = remember { mutableStateListOf<Category>() }

    LaunchedEffect(items) {
        localList.clear()
        localList.addAll(items)
    }

    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            localList.apply {
                add(to.index, removeAt(from.index))
            }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    )

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = localList,
            key = { _, item -> item.id }
        ) { index, category ->
            ReorderableItem(
                state = reorderableLazyListState,
                key = category.id
            ) { isDragging ->
                val shape =
                    if (isDragging) RoundedCornerShape(12.dp) else itemShape(index, localList.size)
                val color =
                    if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
                val elevation = if (isDragging) 6.dp else 0.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .shadow(elevation, shape)
                        .clip(shape)
                        .background(color)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(text = category.name)
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRename(category) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDelete(category) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        val updatedOrder = localList.mapIndexed { i, cat ->
                                            cat.copy(sortOrder = i)
                                        }
                                        onReorderFinished(updatedOrder)
                                    }
                                )
                            ) {
                                Icon(Icons.Default.DragHandle, contentDescription = "Reorder")
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}