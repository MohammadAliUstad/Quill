package com.yugentech.quill.ui.book.bookDetails.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.yugentech.quill.R
import com.yugentech.quill.bookDetails.model.DownloadError
import com.yugentech.quill.database.model.Book
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.database.model.DownloadStatus
import com.yugentech.quill.localization.asString
import com.yugentech.quill.ui.common.components.MaintenanceAwareCover
import com.yugentech.quill.ui.common.util.thumbnailCoverUrl
import com.yugentech.quill.ui.common.util.shelfTitle
import com.yugentech.theme.tokens.dimensions.AppAnimations
import java.util.Locale

/** Size of the cover at the top of the page. */
private val CoverWidth = 136.dp
private val CoverHeight = 204.dp

/** Height of the two action buttons under the cover. */
private val ActionHeight = 56.dp

/** Corner of the action buttons' outer ends (fully round) and of the edges where they meet. */
private val OuterCorner = 28.dp
private val InnerCorner = 8.dp

/**
 * The top of a book's page: the cover beside the title, author and a few facts about the book
 * (where it's from, its language, how long it is), then its two actions as a button group (see
 * [BookActions]). Removing a download lives in the top bar's menu, with deleting the book.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookHeaderContent(
    book: Book,
    topPadding: Dp,
    // The book's source is down for maintenance: until it's back it can't be downloaded (the
    // page says why, in place of the description)
    isSourceDown: Boolean,
    // How far the running download has got, 0..1, when that's known
    downloadProgress: Float?,
    onCategoryClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onReadClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var wasDownloading by remember { mutableStateOf(false) }
    var showCoverDialog by remember { mutableStateOf(false) }

    // The tap when a watched download lands is played by the button, in time with its tick
    LaunchedEffect(book.downloadStatus) {
        if (book.downloadStatus == DownloadStatus.DOWNLOADING) {
            wasDownloading = true
        }
    }

    // A download already running is left to finish or fail on its own
    val isUnavailable = isSourceDown &&
        book.downloadStatus != DownloadStatus.DOWNLOADED &&
        book.downloadStatus != DownloadStatus.DOWNLOADING
    val showError = !isUnavailable &&
        book.downloadStatus == DownloadStatus.FAILED &&
        book.downloadError != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + 16.dp, start = 24.dp, end = 24.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ElevatedCard(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    showCoverDialog = true
                },
                modifier = Modifier
                    .width(CoverWidth)
                    .height(CoverHeight),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
            ) {
                MaintenanceAwareCover(
                    url = book.thumbnailCoverUrl,
                    contentDescription = stringResource(R.string.book_cover),
                    contentScale = ContentScale.Crop,
                    iconSize = 48.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // The title leads from the cover's top edge, the author just under it, and the
            // facts settle at its bottom edge; a long title may stretch the column past the cover
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = CoverHeight)
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SmartMarqueeTitle(title = book.title)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = book.author,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(16.dp))
                BookFacts(book = book)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        BookActions(
            book = book,
            isUnavailable = isUnavailable,
            justDownloaded = wasDownloading,
            downloadProgress = downloadProgress,
            onDownloadClick = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onDownloadClick()
            },
            onReadClick = {
                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onReadClick()
            },
            onCategoryClick = {
                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onCategoryClick()
            }
        )

        AnimatedVisibility(
            visible = showError,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Row(
                modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = book.downloadError?.let { DownloadError.message(it).asString() }.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showCoverDialog) {
        CoverImageDialog(
            coverUrl = book.thumbnailCoverUrl,
            onDismiss = { showCoverDialog = false }
        )
    }
}

/** What the main action is right now. */
internal enum class MainActionState { Unavailable, Download, Downloading, Retry, Read }

/**
 * The book's two actions as one connected pair: fully round at the outer ends, nearly square
 * where they meet, and a pressed one's inner corners round out, so the press shows in its shape.
 *
 * - The main one, the bigger share, is the next thing to do: Download, then Read (or Continue,
 *   once started). Downloading waits, a failed download offers to try again, and while the
 *   source is down it can't be pressed. Its colour eases between states, its icon tells the
 *   download's story (see [DownloadActionIcon]) and its words fade through.
 * - Beside it, tonal, the shelf the book is on (or Add to Library), opening the shelf picker.
 *   Its name fades through when the book moves shelf.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BookActions(
    book: Book,
    isUnavailable: Boolean,
    // The download finished while the page was open, so its tick plays on the way to Read
    justDownloaded: Boolean,
    downloadProgress: Float?,
    onDownloadClick: () -> Unit,
    onReadClick: () -> Unit,
    onCategoryClick: () -> Unit
) {
    val actualState = when {
        isUnavailable -> MainActionState.Unavailable
        book.downloadStatus == DownloadStatus.DOWNLOADED -> MainActionState.Read
        book.downloadStatus == DownloadStatus.DOWNLOADING -> MainActionState.Downloading
        book.downloadStatus == DownloadStatus.FAILED -> MainActionState.Retry
        else -> MainActionState.Download
    }
    // A download that's done in a blink still gets its ring filled and a moment at full
    val paced = rememberPacedDownload(actualState, downloadProgress)
    val state = paced.state
    val colors = MaterialTheme.colorScheme
    val (targetContainer, targetContent) = when (state) {
        MainActionState.Unavailable -> colors.surfaceContainerHighest to colors.onSurfaceVariant
        MainActionState.Downloading -> colors.primaryContainer to colors.onPrimaryContainer
        MainActionState.Retry -> colors.errorContainer to colors.onErrorContainer
        MainActionState.Download, MainActionState.Read -> colors.primary to colors.onPrimary
    }
    val colorSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(targetContainer, colorSpec, label = "mainActionContainer")
    val content by animateColorAsState(targetContent, colorSpec, label = "mainActionContent")

    val mainLabel = when (state) {
        MainActionState.Unavailable -> stringResource(R.string.download_unavailable)
        MainActionState.Download -> stringResource(R.string.download)
        MainActionState.Downloading -> stringResource(R.string.downloading)
        MainActionState.Retry -> stringResource(R.string.retry_download)
        MainActionState.Read -> if (book.progressPercent > 0f) {
            stringResource(R.string.onboarding_continue)
        } else {
            stringResource(R.string.read)
        }
    }

    val isSaved = book.userCategory != null
    val shelfLabel = book.userCategory?.let { shelfTitle(it) } ?: stringResource(R.string.add_to_library)
    val shelfIcon = if (isSaved) Icons.Default.FolderOpen else Icons.Default.LibraryAdd

    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ConnectedButton(
            isStart = true,
            containerColor = container,
            contentColor = content,
            enabled = state != MainActionState.Unavailable && state != MainActionState.Downloading,
            onClick = { if (state == MainActionState.Read) onReadClick() else onDownloadClick() },
            modifier = Modifier.weight(1.25f)
        ) {
            // Arrow, then a wavy ring filling with the download, then a drawn tick, then the book
            DownloadActionIcon(
                state = state,
                progress = paced.progress,
                justDownloaded = justDownloaded
            )
            Spacer(Modifier.width(8.dp))
            // The words swap vertically, as the onboarding button's Continue and Start: the new
            // label pushes the old one out, upward as the book moves on (Download, Downloading,
            // Read) and downward back to Download. The icon beside it keeps its own animation and
            // glides to its new place as the label's width eases, as the onboarding arrow does.
            // Keyed on the words too, so Read turning into Continue swaps as well
            AnimatedContent(
                targetState = state to mainLabel,
                transitionSpec = {
                    val backwards = targetState.first == MainActionState.Download ||
                        targetState.first == MainActionState.Unavailable
                    val direction = if (backwards) -1 else 1
                    // Download to Downloading waits for the arrow to fall out of the button first
                    val wait = if (initialState.first == MainActionState.Download &&
                        targetState.first == MainActionState.Downloading
                    ) ARROW_DROP_MILLIS else 0
                    (slideInVertically(tween(300, delayMillis = wait)) { it * direction } + fadeIn(tween(300, delayMillis = wait)))
                        .togetherWith(slideOutVertically(tween(300, delayMillis = wait)) { -it * direction } + fadeOut(tween(300, delayMillis = wait)))
                        // Unclipped: the label area widens without slicing the longer text; the
                        // button's own shape still bounds it
                        .using(SizeTransform(clip = false) { _, _ -> tween(300, delayMillis = wait) })
                },
                contentAlignment = Alignment.Center,
                label = "mainActionLabel"
            ) { (_, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        ConnectedButton(
            isStart = false,
            containerColor = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
            onClick = onCategoryClick,
            modifier = Modifier.weight(1f)
        ) {
            AnimatedContent(
                targetState = shelfLabel to shelfIcon,
                transitionSpec = { fadeThrough() },
                label = "shelfAction"
            ) { (label, icon) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

/**
 * One half of a connected pair: fully round at its outer end, nearly square where it meets the
 * other. Pressed, the inner corners round out too, so the press reads in the shape itself.
 * [isStart] is the half at the start of the row (mirrored in right-to-left layouts).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectedButton(
    isStart: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val inner by animateDpAsState(
        targetValue = if (isPressed) OuterCorner else InnerCorner,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "connectedButtonCorner"
    )
    val shape = if (isStart) {
        RoundedCornerShape(topStart = OuterCorner, bottomStart = OuterCorner, topEnd = inner, bottomEnd = inner)
    } else {
        RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = OuterCorner, bottomEnd = OuterCorner)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier.height(ActionHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            content = content
        )
    }
}

/**
 * A few facts about the book as small pills, wrapping onto a second row if need be: where it's from, its language in
 * the reader's own, and its length once known (a book not yet downloaded hasn't been counted).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookFacts(book: Book) {
    val appLocale = LocalConfiguration.current.locales[0]
    val language = remember(book.language, appLocale) {
        book.language.takeIf { it.isNotBlank() }
            ?.let { Locale.forLanguageTag(it).getDisplayLanguage(appLocale) }
            ?.takeIf { it.isNotBlank() && !it.equals(book.language, ignoreCase = true) }
            ?.replaceFirstChar { it.titlecase(appLocale) }
    }
    val facts = listOfNotNull(
        when (book.source) {
            BookSource.STANDARD_EBOOKS -> stringResource(R.string.source_standard_name)
            BookSource.USER_IMPORTED -> stringResource(R.string.book_source_imported)
        },
        language,
        book.totalPages.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.book_page_count, it, it) }
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        facts.forEach { fact ->
            Text(
                text = fact,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun CoverImageDialog(
    coverUrl: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = stringResource(R.string.full_book_cover),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
        }
    }
}

/**
 * The book's title in up to [maxLines] lines, wrapped as text normally would; whatever doesn't fit
 * goes on the last line, which scrolls (marquee) instead of being cut off.
 */
@Composable
fun SmartMarqueeTitle(
    title: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3
) {
    var lines by remember(title, maxLines) { mutableStateOf<List<String>>(emptyList()) }

    val visibleStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.25f),
            blurRadius = 4f
        )
    )
    val layoutStyle = visibleStyle.copy(shadow = null)

    Box(modifier = modifier.fillMaxWidth()) {
        // Measured invisibly to find where each line breaks
        Text(
            text = title,
            style = layoutStyle,
            maxLines = maxLines,
            color = Color.Transparent,
            onTextLayout = { layout ->
                val shown = layout.lineCount.coerceAtMost(maxLines)
                lines = buildList {
                    var start = 0
                    for (i in 0 until shown - 1) {
                        val end = layout.getLineEnd(i, visibleEnd = true)
                        add(title.substring(start, end).trim())
                        start = end
                    }
                    add(title.substring(start).trim())
                }.filter { it.isNotEmpty() }
            }
        )

        if (lines.isNotEmpty()) {
            Column {
                lines.forEachIndexed { index, line ->
                    val isLast = index == lines.lastIndex
                    Text(
                        text = line,
                        style = visibleStyle,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground,
                        overflow = TextOverflow.Clip,
                        modifier = if (isLast) Modifier.basicMarquee() else Modifier
                    )
                }
            }
        }
    }
}
