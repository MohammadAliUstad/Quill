package com.yugentech.quill.ui.shelf.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.common.components.loadingPulse

/** How many placeholder covers fill the screen while a catalogue loads. */
private const val SKELETON_COUNT = 18

/**
 * A catalogue grid while its first page loads: pulsing covers laid out exactly as the real
 * [columns] grid lays out its books (same padding, spacing and cover shape), so the books
 * take their places without anything moving.
 */
@Composable
fun BooksGridSkeleton(
    columns: GridCells,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(
            top = topPadding,
            start = 16.dp,
            end = 16.dp,
            bottom = bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize()
    ) {
        items(SKELETON_COUNT) { BookCoverPlaceholder() }
    }
}

/** One pulsing cover, the size and shape of a [StandardBookItem]. */
@Composable
fun BookCoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .clip(RoundedCornerShape(8.dp))
            .loadingPulse()
    )
}
