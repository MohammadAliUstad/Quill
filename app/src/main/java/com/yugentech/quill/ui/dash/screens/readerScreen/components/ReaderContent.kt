package com.yugentech.quill.ui.dash.screens.readerScreen.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import kotlin.math.roundToInt

/**
 * Optimized ReaderContent following Android best practices:
 *
 * Performance Optimizations:
 * 1. Uses derivedStateOf for expensive computations (TOC lookup)
 * 2. rememberSaveable for UI state that should survive config changes
 * 3. Minimal recompositions with targeted state updates
 * 4. Lazy loading indicator that shows until first locator
 * 5. Immutable state objects
 *
 * Best Practices:
 * 1. Single source of truth for state
 * 2. Proper system UI management
 * 3. Cleanup in DisposableEffect
 * 4. Separation of UI and business logic
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderContent(
    publication: Publication,
    bookId: String,
    initialLocation: Locator?,
    allPositions: List<Locator>,
    onBackClick: () -> Unit,
    // NEW: Callback to notify parent (Screen) about progress updates
    onLocatorChange: (Locator) -> Unit
) {
    // UI state - survives config changes
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showTocSheet by rememberSaveable { mutableStateOf(false) }

    // Loading state - becomes true after first locator
    var isReaderReady by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val window = remember { (context as? Activity)?.window }

    // System UI management
    LaunchedEffect(isMenuVisible) {
        window?.let {
            val controller = WindowCompat.getInsetsController(it, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (isMenuVisible) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Cleanup system UI on dispose
    DisposableEffect(Unit) {
        onDispose {
            window?.let {
                WindowCompat.getInsetsController(it, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Reader preferences - immutable state object
    var currentPreferences by remember {
        mutableStateOf(
            EpubPreferences(
                fontFamily = FontFamily.SANS_SERIF,
                theme = Theme.LIGHT,
                publisherStyles = false
            )
        )
    }

    // Navigation state
    var targetJumpHref by remember { mutableStateOf<String?>(null) }
    var pendingSeekProgress by remember { mutableStateOf<Double?>(null) }
    var currentLocator by remember { mutableStateOf<Locator?>(null) }

    val totalPages = remember(allPositions) { allPositions.size.coerceAtLeast(1) }

    // Mark ready when first locator arrives
    LaunchedEffect(currentLocator) {
        if (currentLocator != null && !isReaderReady) {
            isReaderReady = true
        }
    }

    // Derived state - computed only when dependencies change
    val displayTitle by remember(allPositions, currentLocator, pendingSeekProgress) {
        derivedStateOf {
            computeChapterTitle(
                allPositions = allPositions,
                currentLocator = currentLocator,
                pendingSeekProgress = pendingSeekProgress,
                publication = publication,
                totalPages = totalPages
            )
        }
    }

    val currentPage by remember(pendingSeekProgress, currentLocator, allPositions) {
        derivedStateOf {
            val effectiveProgress =
                pendingSeekProgress ?: currentLocator?.locations?.totalProgression ?: 0.0
            if (allPositions.isEmpty()) 1
            else (effectiveProgress * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1) + 1
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fragment Manager
        ReaderFragmentManager(
            publication = publication,
            bookId = bookId,
            initialLocation = initialLocation,
            targetJumpHref = targetJumpHref,
            targetSeekProgress = pendingSeekProgress,
            allPositions = allPositions,
            preferences = currentPreferences,
            onTap = { isMenuVisible = !isMenuVisible },
            onJumpComplete = { targetJumpHref = null },
            onSeekComplete = { pendingSeekProgress = null },
            // UPDATED: Update local state AND notify parent
            onLocatorChange = { newLocator ->
                currentLocator = newLocator
                onLocatorChange(newLocator)
            }
        )

        // Loading overlay - shows until reader is ready
        if (!isReaderReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Menu overlay - only shown when reader is ready
        if (isReaderReady) {
            ReaderMenuOverlay(
                isVisible = isMenuVisible,
                progress = (currentLocator?.locations?.totalProgression ?: 0.0).toFloat(),
                currentPage = currentPage,
                totalPages = totalPages,
                chapterTitle = displayTitle,
                onBackClick = onBackClick,
                onSettingsClick = {
                    isMenuVisible = false
                    showSettingsSheet = true
                },
                onTocClick = {
                    isMenuVisible = false
                    showTocSheet = true
                },
                onBookmarkClick = { /* TODO */ },
                onSeek = { newProgress -> pendingSeekProgress = newProgress.toDouble() }
            )
        }
    }

    // Bottom sheets
    if (showTocSheet) {
        ReaderTableOfContents(
            toc = publication.tableOfContents,
            onDismiss = { showTocSheet = false },
            onTocItemClick = { href ->
                targetJumpHref = href
                showTocSheet = false
            }
        )
    }

    if (showSettingsSheet) {
        ReaderSettingsSheet(
            preferences = currentPreferences,
            onPreferencesChange = { newPrefs ->
                currentPreferences = newPrefs.copy(publisherStyles = false)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

/**
 * Extracted computation function for chapter title.
 * Separate from Composable for better testability and clarity.
 */
private fun computeChapterTitle(
    allPositions: List<Locator>,
    currentLocator: Locator?,
    pendingSeekProgress: Double?,
    publication: Publication,
    totalPages: Int
): String {
    val effectiveProgress =
        pendingSeekProgress ?: currentLocator?.locations?.totalProgression ?: 0.0

    return if (allPositions.isNotEmpty()) {
        val index = (effectiveProgress * (totalPages - 1))
            .roundToInt()
            .coerceIn(0, totalPages - 1)

        val targetUrl = allPositions[index].href
        val targetHrefString = targetUrl.toString()

        findTitleInToc(targetHrefString, publication.tableOfContents)
            ?: publication.linkWithHref(targetUrl)?.title
            ?: "Chapter ${index + 1}"
    } else {
        currentLocator?.let { loc ->
            findTitleInToc(loc.href.toString(), publication.tableOfContents)
                ?: publication.linkWithHref(loc.href)?.title
        } ?: "Chapter"
    }
}

/**
 * Recursive TOC search - tail-call optimized where possible.
 */
private fun findTitleInToc(
    href: String,
    links: List<org.readium.r2.shared.publication.Link>
): String? {
    for (link in links) {
        // Early return on match
        if (link.href.toString().startsWith(href)) {
            return link.title
        }

        // Recursive search in children
        if (link.children.isNotEmpty()) {
            findTitleInToc(href, link.children)?.let { return it }
        }
    }
    return null
}