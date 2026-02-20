package com.yugentech.quill.reader.reader.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderContent(
    publication: Publication,
    bookId: String,
    initialLocation: Locator?,
    allPositions: List<Locator>,
    totalPages: Int,
    onBackClick: () -> Unit,
    onLocatorChange: (Locator) -> Unit
) {
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showTocSheet by rememberSaveable { mutableStateOf(false) }

    var currentPreferences by remember { mutableStateOf(ReaderDefaults.getPreferences()) }
    var targetJumpHref by remember { mutableStateOf<String?>(null) }
    var pendingSeekProgress by remember { mutableStateOf<Double?>(null) }
    var currentLocator by remember { mutableStateOf<Locator?>(null) }

    SystemBarsImmersiveMode(isVisible = isMenuVisible)

    val displayTitle by remember(currentLocator) {
        derivedStateOf {
            currentLocator?.title ?: publication.metadata.title ?: "Chapter"
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = Color(
            currentPreferences.backgroundColor?.int
                ?: ReaderDefaults.getPreferences().backgroundColor!!.int
        ),
        label = "ReaderBg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
    ) {
        ReadiumEngine(
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
            onLocatorChange = { newLocator ->
                currentLocator = newLocator
                onLocatorChange(newLocator)
            }
        )

        ReaderMenuOverlay(
            isVisible = isMenuVisible,
            progress = (currentLocator?.locations?.totalProgression ?: 0.0).toFloat(),
            totalPages = totalPages,
            chapterTitle = displayTitle,
            onBackClick = onBackClick,
            onSettingsClick = { isMenuVisible = false; showSettingsSheet = true },
            onTocClick = { isMenuVisible = false; showTocSheet = true },
            onSeek = { pendingSeekProgress = it.toDouble() }
        )
    }

    if (showTocSheet) {
        ReaderTableOfContents(
            toc = publication.tableOfContents,
            onDismiss = { showTocSheet = false },
            onTocItemClick = { href -> targetJumpHref = href; showTocSheet = false }
        )
    }

    if (showSettingsSheet) {
        ReaderSettingsSheet(
            preferences = currentPreferences,
            onPreferencesChange = { currentPreferences = it.copy(publisherStyles = false) },
            onDismiss = { showSettingsSheet = false }
        )
    }
}