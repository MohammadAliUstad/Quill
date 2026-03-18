package com.yugentech.quill.ui.tabs.discoverScreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yugentech.quill.database.model.Book
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    if (books.isEmpty()) return

    // --- NEW: Group books into pairs ---
    val pairedBooks = remember(books) { books.chunked(2) }

    // 1. Set pageCount to a very large number for infinite looping
    val pageCount = Int.MAX_VALUE

    // 2. Start in the middle of the large number, but force it to perfectly align with Index 0
    val startPage = (pageCount / 2) - ((pageCount / 2) % pairedBooks.size)
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { pageCount }
    )

    // Auto-scroll logic: always moves index forward
    LaunchedEffect(pairedBooks.size) {
        if (pairedBooks.size > 1) {
            while (true) {
                delay(5000)
                // Simply increment the page; HorizontalPager handles the animation forward
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage + 1,
                    animationSpec = tween(800)
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            // Slightly reduced padding to give the two books more room to breathe
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { index ->
            // 3. Map the large 'index' back to your paired list
            val actualIndex = index % pairedBooks.size
            val pair = pairedBooks[actualIndex]

            // --- NEW: Display the pair in a Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Book 1
                HeroCarouselCard(
                    book = pair[0],
                    onBookClick = onBookClick,
                    modifier = Modifier.weight(1f) // Takes up half the row
                )

                // Book 2 (Check if it exists to handle odd-numbered lists)
                if (pair.size > 1) {
                    HeroCarouselCard(
                        book = pair[1],
                        onBookClick = onBookClick,
                        modifier = Modifier.weight(1f) // Takes up half the row
                    )
                } else {
                    // Empty space if there's an odd number of books
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // --- Centered Pagination Dots ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Iterate over the pairs, not individual books
            pairedBooks.forEachIndexed { index, _ ->
                val isSelected = (pagerState.currentPage % pairedBooks.size) == index

                val animatedWidth by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dot_width"
                )
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant,
                    label = "dot_color"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(animatedWidth)
                        .clip(CircleShape)
                        .background(animatedColor)
                )
            }
        }
    }
}