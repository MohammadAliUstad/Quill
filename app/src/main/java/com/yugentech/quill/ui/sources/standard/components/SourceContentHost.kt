package com.yugentech.quill.ui.sources.standard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.yugentech.quill.R
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.localization.asString
import com.yugentech.quill.sources.shared.SourceContent
import com.yugentech.quill.ui.shared.search.NoSearchResults

/**
 * What a source screen shows below its header, exactly as its view model says: the skeleton
 * grid, the books, the empty state, the error, or that the source is down for maintenance.
 *
 * Moving between those crossfades, with no zoom: the skeleton and the grid lay out the same
 * cells, so each cover takes its placeholder's place, and the grid's own entrance ripples the
 * covers in. Only fetched books ripple, the ones replacing the skeleton: cached books are just
 * there, since there was nothing to wait for. Before the cache answers nothing is drawn, so a
 * cached list fades straight in without the skeleton flashing first.
 *
 * Each list gets its own grid ([SourceContent.Books.listKey]): another category or search starts
 * at the top with its own entrance, while a refresh or a new page of the same list updates the
 * grid in place, covers staying put.
 */
@Composable
fun SourceContentHost(
    content: SourceContent,
    skeletonColumns: GridCells,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    books: @Composable (List<Book>) -> Unit
) {
    val entrances = remember { EntranceTracker() }
    entrances.observe(content)

    AnimatedContent(
        targetState = content,
        // Only a change of state animates; new books within the books state don't
        contentKey = { it::class },
        transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(150)) },
        label = "sourceContent",
        modifier = modifier.fillMaxSize()
    ) { state ->
        when (state) {
            SourceContent.Pending -> Box(Modifier.fillMaxSize())

            SourceContent.Loading -> BooksGridSkeleton(
                columns = skeletonColumns,
                topPadding = topPadding,
                bottomPadding = bottomPadding
            )

            is SourceContent.Books -> key(state.listKey) {
                CompositionLocalProvider(
                    LocalGridEntranceEnabled provides (state.listKey == entrances.rippleList)
                ) {
                    books(state.books)
                }
            }

            is SourceContent.Empty -> SourceEmptyState(
                title = state.message.asString(),
                message = null,
                isError = false,
                modifier = Modifier.padding(top = topPadding)
            )

            is SourceContent.Error -> SourceEmptyState(
                title = stringResource(R.string.something_went_wrong),
                message = state.message.asString(),
                isError = true,
                modifier = Modifier.padding(top = topPadding)
            )

            SourceContent.NoSearchResults -> NoSearchResults(
                modifier = Modifier.padding(top = topPadding)
            )

            is SourceContent.Maintenance -> SourceMaintenanceState(
                backAfter = state.backAfter,
                modifier = Modifier.padding(top = topPadding)
            )
        }
    }
}

/**
 * Which list, if any, has just arrived after the skeleton and so ripples in. A list shown from
 * the cache never passes through the skeleton, so it isn't marked and appears at once.
 *
 * Plain fields rather than state: it's worked out from [SourceContentHost]'s content each time
 * that changes, and reading it never needs to recompose anything.
 */
private class EntranceTracker {
    /** The skeleton is up, so the next books to arrive were fetched. */
    private var awaitingBooks = false

    /** The list that arrived after the skeleton. */
    var rippleList: String? = null
        private set

    fun observe(content: SourceContent) {
        when (content) {
            SourceContent.Loading -> awaitingBooks = true
            is SourceContent.Books -> {
                if (awaitingBooks) rippleList = content.listKey
                else if (content.listKey != rippleList) rippleList = null
                awaitingBooks = false
            }
            else -> awaitingBooks = false
        }
    }
}
