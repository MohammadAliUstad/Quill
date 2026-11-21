package com.yugentech.quill.ui.shared.maintenance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.yugentech.quill.database.model.BookSource

/**
 * The book sources down for maintenance right now, provided once at the app's root from
 * SourceMaintenanceMonitor, so any cover can tell without each screen passing it down.
 */
val LocalSourcesDown = compositionLocalOf<Set<BookSource>> { emptySet() }

/** The source whose server a cover is fetched from, judged by its address. */
private fun sourceOfCover(url: String?): BookSource? = when {
    url == null -> null
    "standardebooks.org" in url -> BookSource.STANDARD_EBOOKS
    else -> null
}

/**
 * A book cover that knows about maintenance: if it couldn't load because its source's server is
 * down for maintenance, a maintenance icon takes its place instead of an empty box. Once the
 * server's back the cover loads again by itself.
 *
 * [imageAlpha] fades only the image (a cover fading in over its own placeholder), not the icon.
 */
@Composable
fun MaintenanceAwareCover(
    url: String?,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    imageAlpha: Float = 1f,
    onState: ((AsyncImagePainter.State) -> Unit)? = null
) {
    val isSourceDown = sourceOfCover(url)?.let { it in LocalSourcesDown.current } == true
    var state by remember(url) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    Box(modifier = modifier) {
        // A fresh request when the server comes back: the failed one isn't retried on its own
        key(isSourceDown) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onState = {
                    state = it
                    onState?.invoke(it)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha)
            )
        }

        // Fades in and out rather than snapping, so it settles in like the cover would
        AnimatedVisibility(
            visible = isSourceDown && state is AsyncImagePainter.State.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Construction,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
