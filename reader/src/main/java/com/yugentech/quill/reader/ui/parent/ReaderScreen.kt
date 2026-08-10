package com.yugentech.quill.reader.ui.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yugentech.quill.reader.viewmodel.QuickViewModel
import com.yugentech.quill.reader.pref.model.QuillPreferences
import com.yugentech.quill.reader.ui.components.engine.ReaderDefaults
import com.yugentech.quill.reader.ui.components.engine.ReadiumEngine
import com.yugentech.quill.reader.ui.components.overlay.parent.ReaderAction
import com.yugentech.quill.reader.ui.components.overlay.parent.ReaderMenuOverlay
import com.yugentech.quill.reader.ui.components.settingsSheet.SettingsSheet
import com.yugentech.quill.reader.ui.components.soundSheet.SoundSelectionSheet
import com.yugentech.quill.reader.ui.components.tocSheet.TocSheet
import com.yugentech.quill.reader.viewmodel.ReaderUiState
import com.yugentech.quill.reader.viewmodel.ReaderViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    uiState: ReaderUiState,
    onBackClick: () -> Unit,
    preferences: QuillPreferences,
    statusBarHeight: Dp = 0.dp,
    onPreferencesChange: (EpubPreferences) -> Unit,
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
            viewModel = viewModel,
            state = uiState,
            onBackClick = onBackClick,
            preferences = preferences,
            statusBarHeight = statusBarHeight,
            onPreferencesChange = onPreferencesChange,
            onLocatorChange = onLocatorChange,
            onMenuVisibilityChange = onMenuVisibilityChange
        )
    }
}

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSuccess(
    viewModel: ReaderViewModel,
    state: ReaderUiState.Success,
    onBackClick: () -> Unit,
    preferences: QuillPreferences,
    statusBarHeight: Dp,
    onPreferencesChange: (EpubPreferences) -> Unit,
    onLocatorChange: (Locator) -> Unit,
    onMenuVisibilityChange: (Boolean) -> Unit
) {
    val quickViewModel: QuickViewModel = koinViewModel()
    val airaUiState by quickViewModel.uiState.collectAsState()
    val isPro = airaUiState.isPro
    val isReady = airaUiState.isReady

    val screenState = rememberReaderScreenState(
        publication = state.publication,
        allPositions = state.allPositions,
        totalPages = state.totalPages,
        initialLocator = state.initialLocator
    )

    val dbHighlights by viewModel.highlights.collectAsState()

    val activeDecorations = remember(dbHighlights) {
        dbHighlights.mapNotNull { entity ->
            try {
                Locator.fromJSON(JSONObject(entity.locatorJson))?.let { locator ->
                    Decoration(
                        id = entity.id,
                        locator = locator,
                        style = Decoration.Style.Highlight(tint = entity.colorInt)
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    var pendingHighlightLocator by remember { mutableStateOf<Locator?>(null) }
    val sheetState = rememberModalBottomSheetState()

    var highlightToDelete by remember { mutableStateOf<Decoration?>(null) }

    LaunchedEffect(state.bookId) {
        quickViewModel.observeIndexingStatus(state.bookId)
    }

    LaunchedEffect(screenState.isMenuVisible, screenState.showAiraPeek) {
        onMenuVisibilityChange(screenState.isMenuVisible || screenState.showAiraPeek)
    }

    LaunchedEffect(
        screenState.isMenuVisible,
        screenState.isBrightnessInteracting,
        screenState.isScrubbing,
        screenState.showAiraPeek
    ) {
        if (screenState.isMenuVisible && !screenState.isBrightnessInteracting && !screenState.isScrubbing && !screenState.showAiraPeek) {
            delay(4000L)
            screenState.isMenuVisible = false
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = Color(
            preferences.epub.backgroundColor?.int
                ?: ReaderDefaults.getPreferences().backgroundColor!!.int
        ),
        label = "ReaderBg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
    ) {
        val isPagedMode = preferences.epub.scroll == false
        // Use a fixed padding in Paged mode to avoid shifting when system bars are toggled
        val engineModifier = if (isPagedMode) Modifier.padding(top = statusBarHeight) else Modifier

        AnimatedContent(
            targetState = preferences.epub.scroll ?: true,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)).togetherWith(fadeOut(animationSpec = tween(500)))
            },
            label = "ReaderModeTransition"
        ) { targetScroll ->
            ReadiumEngine(
                modifier = engineModifier,
                publication = state.publication,
                bookId = state.bookId,
                initialLocation = screenState.currentLocator ?: state.initialLocator,
                targetJumpHref = screenState.targetJumpHref,
                targetSeekProgress = screenState.pendingSeekProgress,
                targetLocator = screenState.targetLocator,
                allPositions = state.allPositions,
                preferences = preferences.epub.copy(scroll = targetScroll),
                commands = viewModel.commands,
                isPro = isPro,
                isAiraReady = isReady,
                decorations = activeDecorations,
                onTap = { screenState.toggleMenu() },
                onAskAira = { text ->
                    screenState.showAira(text)
                    quickViewModel.clearResponse()
                },
                onSelectionAction = { locator ->
                    pendingHighlightLocator = locator
                },
                onDecorationTapped = { tappedDecoration ->
                    highlightToDelete = tappedDecoration
                },
                onJumpComplete = { screenState.targetJumpHref = null },
                onSeekComplete = { screenState.pendingSeekProgress = null },
                onTargetLocatorComplete = { screenState.targetLocator = null },
                onLocatorChange = { newLocator ->
                    screenState.handleLocatorChange(newLocator)
                    onLocatorChange(newLocator)
                }
            )
        }

        // --- NIGHT LIGHT OVERLAY ---
        if (preferences.nightLight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE58F00).copy(alpha = 0.15f))
                    .zIndex(0.5f)
            )
        }

        ReaderMenuOverlay(
            modifier = Modifier.zIndex(1f),
            isPro = isPro,
            isVisible = screenState.isMenuVisible || screenState.showAiraPeek,
            showBottomControls = !screenState.showAiraPeek,
            showAiraPeek = screenState.showAiraPeek,
            readerOverlayState = screenState.overlayState,
            airaUiState = airaUiState,
            onAction = { action ->
                when (action) {
                    is ReaderAction.OnBackClick -> onBackClick()
                    is ReaderAction.OnSettingsClick -> {
                        screenState.isMenuVisible = false
                        screenState.showSettingsSheet = true
                    }

                    is ReaderAction.OnTocClick -> {
                        screenState.isMenuVisible = false
                        screenState.showTocSheet = true
                    }

                    is ReaderAction.OnSoundClick -> {
                        screenState.isMenuVisible = false
                        screenState.showSoundSheet = true
                    }

                    is ReaderAction.OnSeek -> {
                        screenState.isExplicitJump = true
                        screenState.pendingSeekProgress = action.progress.toDouble()
                    }

                    is ReaderAction.OnScrubStart -> screenState.isScrubbing = true
                    is ReaderAction.OnScrubEnd -> screenState.isScrubbing = false
                    is ReaderAction.OnBrightnessInteraction -> screenState.isBrightnessInteracting =
                        action.isInteracting

                    is ReaderAction.OnAskAiraClick -> screenState.showAira(null)
                    is ReaderAction.OnAiraDismiss -> {
                        screenState.dismissAira()
                        quickViewModel.clearResponse()
                    }

                    is ReaderAction.OnAiraSend -> quickViewModel.ask(state.bookId, action.question)
                    is ReaderAction.OnQuickAction -> quickViewModel.handleQuickPrompt(
                        state.bookId,
                        action.prompt
                    )

                    is ReaderAction.OnStopGeneration -> quickViewModel.stopGeneration()
                    is ReaderAction.OnClearSelection -> screenState.selectedText = null
                }
            }
        )
    }

    if (pendingHighlightLocator != null) {
        HighlightSheet(
            sheetState = sheetState,
            onDismiss = { pendingHighlightLocator = null },
            onSave = { colorInt ->
                val locator = pendingHighlightLocator!!

                viewModel.addHighlight(
                    bookId = state.bookId,
                    locatorJson = locator.toJSON().toString(),
                    colorInt = colorInt
                )

                pendingHighlightLocator = null
            }
        )
    }

    if (highlightToDelete != null) {
        AlertDialog(
            onDismissRequest = { highlightToDelete = null },
            title = {
                Text(
                    text = "Delete Annotation",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this annotation? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.deleteHighlight(highlightToDelete!!.id)
                        highlightToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { highlightToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (screenState.showTocSheet) {
        TocSheet(
            toc = state.publication.tableOfContents,
            currentHref = screenState.currentLocator?.href?.toString(),
            onDismiss = { screenState.showTocSheet = false },
            onTocItemClick = { href ->
                screenState.isExplicitJump = true
                screenState.targetJumpHref = href
                screenState.showTocSheet = false
            }
        )
    }

    if (screenState.showSoundSheet) {
        val activeSound by viewModel.activeSound.collectAsState()
        val volume by viewModel.soundVolume.collectAsState()

        SoundSelectionSheet(
            activeSound = activeSound,
            volume = volume,
            onSoundToggle = { viewModel.toggleBackgroundSound(it) },
            onVolumeChange = { viewModel.updateSoundVolume(it) },
            onDismiss = { screenState.showSoundSheet = false }
        )
    }

    if (screenState.showSettingsSheet) {
        SettingsSheet(
            preferences = preferences,
            onPreferencesChange = {
                onPreferencesChange(it.copy(publisherStyles = false))
            },
            onVolumeNavigationChange = { viewModel.updateVolumeNavigation(it) },
            onNightLightChange = { viewModel.updateNightLight(it) },
            onDismiss = { screenState.showSettingsSheet = false }
        )
    }
}
