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
    onBookClick: (Book) -> Unit,
    onVisibleBooksChanged: (List<Book>) -> Unit = {}
) {
    if (books.isEmpty()) return

    val pairedBooks = remember(books) { books.chunked(2) }
    val pageCount = Int.MAX_VALUE
    val startPage = (pageCount / 2) - ((pageCount / 2) % pairedBooks.size)

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { pageCount }
    )

    // Tells the screen which books are showing, for its parallax background
    LaunchedEffect(pagerState.currentPage, pairedBooks) {
        if (pairedBooks.isNotEmpty()) {
            val actualIndex = pagerState.currentPage % pairedBooks.size
            onVisibleBooksChanged(pairedBooks[actualIndex])
        }
    }

    // Auto-scroll logic: only runs when the user is NOT interacting
    LaunchedEffect(pairedBooks.size, pagerState.isScrollInProgress) {
        if (pairedBooks.size > 1 && !pagerState.isScrollInProgress) {
            while (true) {
                delay(5000)
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
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { index ->
            val actualIndex = index % pairedBooks.size
            val pair = pairedBooks[actualIndex]

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeroCarouselCard(
                    book = pair[0],
                    onBookClick = onBookClick,
                    modifier = Modifier.weight(1f)
                )

                if (pair.size > 1) {
                    HeroCarouselCard(
                        book = pair[1],
                        onBookClick = onBookClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        HeroPageDots(
            count = pairedBooks.size,
            selected = pagerState.currentPage % pairedBooks.size
        )
    }
}

/**
 * How many pages the carousel has once loaded: DiscoverViewModel's 10 hero books, two to a page.
 * The loading skeleton draws this many dots, so the indicator doesn't change when the books arrive.
 */
const val HERO_CAROUSEL_PAGES = 5

/**
 * The carousel's page indicator: 8dp dots 6dp apart, start-aligned 16dp in. The current page's dot
 * stretches to 24dp on a bouncy spring and turns the primary colour; the others are outlineVariant.
 * The loading skeleton draws this same indicator (first page selected) rather than a pulsing
 * placeholder of it, so it already looks loaded and stays put when the cards arrive.
 */
@Composable
fun HeroPageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isSelected = selected == index

            val animatedWidth by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "dot_width"
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
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