package com.yugentech.quill.reader.ui.reader.parent

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yugentech.quill.aira.aira.viewmodel.AiraViewModel
import com.yugentech.quill.reader.ReaderUiState
import com.yugentech.quill.reader.components.ReaderDefaults
import com.yugentech.quill.reader.components.ReaderMenuOverlay
import com.yugentech.quill.reader.components.ReadiumEngine
import com.yugentech.quill.reader.ui.reader.menuOverlay.brightnessSlider.BrightnessSlider
import com.yugentech.quill.reader.ui.reader.menuOverlay.settingsSheet.SettingsSheet
import com.yugentech.quill.reader.ui.reader.menuOverlay.tocSheet.TocSheet
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

private const val MENU_AUTO_HIDE_MS = 4000L

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onBackClick: () -> Unit,
    onLocatorChange: (Locator) -> Unit,
    onMenuVisibilityChange: (Boolean) -> Unit = {}
) {
    when (uiState) {
        is ReaderUiState.Idle -> Unit
        is ReaderUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${uiState.message}", color = Color.Red)
            }
        }
        is ReaderUiState.Success -> ReaderSuccess(
            state = uiState,
            onBackClick = onBackClick,
            onLocatorChange = onLocatorChange,
            onMenuVisibilityChange = onMenuVisibilityChange
        )
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
private fun ReaderSuccess(
    state: ReaderUiState.Success,
    onBackClick: () -> Unit,
    onLocatorChange: (Locator) -> Unit,
    onMenuVisibilityChange: (Boolean) -> Unit
) {
    val airaViewModel: AiraViewModel = koinViewModel()
    val airaUiState by airaViewModel.uiState.collectAsState()

    LaunchedEffect(state.bookId) {
        airaViewModel.initForBook(state.bookId)
    }

    var isMenuVisible by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showTocSheet by rememberSaveable { mutableStateOf(false) }
    var isBrightnessInteracting by remember { mutableStateOf(false) }

    var showAiraPeek by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf<String?>(null) } // NEW: Tracks selected text

    var currentPreferences by remember { mutableStateOf(ReaderDefaults.getPreferences()) }
    var targetJumpHref by remember { mutableStateOf<String?>(null) }
    var pendingSeekProgress by remember { mutableStateOf<Double?>(null) }
    var currentLocator by remember { mutableStateOf<Locator?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }

    LaunchedEffect(isMenuVisible, showAiraPeek) {
        onMenuVisibilityChange(isMenuVisible || showAiraPeek)
    }

    LaunchedEffect(isMenuVisible, isBrightnessInteracting, isScrubbing, showAiraPeek) {
        if (isMenuVisible && !isBrightnessInteracting && !isScrubbing && !showAiraPeek) {
            delay(MENU_AUTO_HIDE_MS)
            isMenuVisible = false
        }
    }

    val displayTitle by remember(currentLocator) {
        derivedStateOf {
            currentLocator?.title ?: state.publication.metadata.title ?: "Chapter"
        }
    }

    val chapterPagesLeft by remember(currentLocator, state.allPositions) {
        derivedStateOf {
            val href = currentLocator?.href ?: return@derivedStateOf 0
            val chapterPositions = state.allPositions.filter { it.href == href }
            val currentProgression = currentLocator?.locations?.progression ?: 0.0
            val currentIndex = chapterPositions
                .indexOfLast { (it.locations.progression ?: 0.0) <= currentProgression }
                .coerceAtLeast(0)
            (chapterPositions.size - 1 - currentIndex).coerceAtLeast(0)
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
            publication = state.publication,
            bookId = state.bookId,
            initialLocation = state.initialLocator,
            targetJumpHref = targetJumpHref,
            targetSeekProgress = pendingSeekProgress,
            allPositions = state.allPositions,
            preferences = currentPreferences,
            onTap = {
                if (showAiraPeek) {
                    showAiraPeek = false
                    selectedText = null // Clear selection when tapping away
                }
                else isMenuVisible = !isMenuVisible
            },
            onAskAira = { text ->
                selectedText = text // Receive the highlighted text
                showAiraPeek = true
            },
            onJumpComplete = { targetJumpHref = null },
            onSeekComplete = { pendingSeekProgress = null },
            onLocatorChange = { newLocator ->
                currentLocator = newLocator
                onLocatorChange(newLocator)
            }
        )

        ReaderMenuOverlay(
            isVisible = isMenuVisible || showAiraPeek,
            showBottomControls = !showAiraPeek,
            showAiraPeek = showAiraPeek,
            progress = (currentLocator?.locations?.totalProgression ?: 0.0).toFloat(),
            totalPages = state.totalPages,
            chapterTitle = displayTitle,
            chapterPagesLeft = chapterPagesLeft,
            onBackClick = onBackClick,
            onSettingsClick = { isMenuVisible = false; showSettingsSheet = true },
            onTocClick = { isMenuVisible = false; showTocSheet = true },
            onSeek = { pendingSeekProgress = it.toDouble() },
            onScrubStart = { isScrubbing = true },
            onScrubEnd = { isScrubbing = false },
            bookTitle = state.publication.metadata.title ?: "Book",
            selectedText = selectedText, // Pass text down
            onAskAiraClick = {
                selectedText = null // Clear old selections for general chat
                showAiraPeek = true
            },
            onAiraDismiss = {
                showAiraPeek = false
                selectedText = null // Clear when dismissed
            },
            airaUiState = airaUiState,
            onAiraSend = { question -> airaViewModel.ask(question) }
        )

        if (!showAiraPeek) {
            BrightnessSlider(
                isVisible = isMenuVisible,
                onDragStart = { isBrightnessInteracting = true },
                onDragEnd = { isBrightnessInteracting = false },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
        }
    }

    if (showTocSheet) {
        TocSheet(
            toc = state.publication.tableOfContents,
            currentHref = currentLocator?.href?.toString(),
            onDismiss = { showTocSheet = false },
            onTocItemClick = { href -> targetJumpHref = href; showTocSheet = false }
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            preferences = currentPreferences,
            onPreferencesChange = { currentPreferences = it.copy(publisherStyles = false) },
            onDismiss = { showSettingsSheet = false }
        )
    }
}