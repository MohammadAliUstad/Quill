package com.yugentech.quill.reader.ui.components.aira

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.aira.util.VoiceOutputManager
import com.yugentech.quill.reader.viewmodel.quick.QuickUiState
import com.yugentech.quill.reader.ui.components.aira.components.AiraPeekHeader
import com.yugentech.quill.reader.ui.components.aira.components.InputBar
import com.yugentech.quill.reader.ui.components.aira.components.PeekDragHandle
import com.yugentech.quill.reader.ui.components.aira.components.PeekResponseArea
import com.yugentech.quill.reader.ui.components.aira.components.QuotaLimitBar
import com.yugentech.quill.reader.ui.components.aira.components.resolveChips
import com.yugentech.theme.service.HapticService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.readium.r2.shared.publication.Locator

private val STATUS_BAR_CLEARANCE = 16.dp
private val EDGE_FADE_HEIGHT = 20.dp
private val MIN_SHEET_HEIGHT = 220.dp
private val MAX_DRAG_UP_SLACK = 16.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiraPeekBar(
    isVisible: Boolean,
    selectedText: String? = null,
    selectedTextLocator: Locator? = null,
    currentChapterIndex: Int = 0,
    airaUiState: QuickUiState,
    onQuickAction: (QuickPrompt) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDismiss: () -> Unit,
    onStop: () -> Unit
) {
    val haptic = koinInject<HapticService>()
    val view = LocalView.current
    val voiceOutputManager: VoiceOutputManager = koinInject()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Tapping outside closes the peek bar via AnimatedVisibility, which disposes its content --
    // including PeekResponseArea's TypewriterText and whatever reveal progress it had. Remembered
    // here, above that AnimatedVisibility, so it survives the close/reopen and a message that
    // already finished typing out once shows up instantly instead of replaying from scratch.
    var lastRevealedResponse by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            voiceOutputManager.stop()
        }
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var isFocused by remember { mutableStateOf(false) }
    var currentGreeting by remember { mutableStateOf("") }

    val contentToActUpon = remember(airaUiState.response, airaUiState.error, currentGreeting) {
        airaUiState.error ?: airaUiState.response ?: currentGreeting
    }

    val onCopyResponse = {
        if (contentToActUpon.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Aira Content", contentToActUpon)
            clipboard.setPrimaryClip(clip)
        }
    }

    val activeChips = remember(selectedText, currentChapterIndex, selectedTextLocator) {
        resolveChips(selectedText, currentChapterIndex, selectedTextLocator)
    }

    var enforceLimitUi by remember { mutableStateOf(!airaUiState.canSendQuery) }

    // null = automatic, content-driven height. Once the user drags the handle, this becomes
    // an explicit override that sticks for the rest of this peek bar session, regardless of
    // how the content changes afterward -- reset back to automatic the next time it opens.
    var manualHeightPx by remember { mutableStateOf<Float?>(null) }
    var measuredHeightPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // Only a real result is worth resizing around -- the empty chips/greeting screen has
    // nothing whose size the user would want to adjust.
    val hasResponseContent = airaUiState.response != null || airaUiState.imagePath != null

    // Natural (unclamped) heights of each sibling, tracked independently of whatever height the
    // user has manually forced the sheet to -- these feed the drag handle's upward limit so it
    // never opens more than MAX_DRAG_UP_SLACK of empty space below the actual content.
    // Seeded with a reasonable guess so the header-sized top padding on the response content
    // (added below, so the header can overlay it) is roughly correct from the very first frame
    // instead of visibly snapping once the real measurement lands.
    var headerHeightPx by remember { mutableStateOf(with(density) { 56.dp.toPx() }) }
    var inputRowHeightPx by remember { mutableStateOf(0f) }
    // The response Box's onSizeChanged is nested *inside* its own verticalScroll modifier, so
    // it reports the full, unclipped content height (not the visible viewport) -- exactly what's
    // needed here, with no separate accounting for scrolled-away overflow.
    var responseContentHeightPx by remember { mutableStateOf(0f) }
    // Hoisted so the drag handle's callback (defined further up the tree) can read it.
    val responseScrollState = rememberScrollState()
    val maxDragUpSlackPx = with(density) { MAX_DRAG_UP_SLACK.toPx() }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            enforceLimitUi = !airaUiState.canSendQuery
        } else {
            inputText = ""
            manualHeightPx = null
        }
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) { if (!isImeVisible) focusManager.clearFocus() }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liftDp = (imeBottom - navBottom).coerceAtLeast(0.dp)
    val kbFraction = (imeBottom / 300.dp).coerceIn(0f, 1f)
    val horizontalPadding = lerp(16.dp, 8.dp, kbFraction)

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    // The whole sheet gets shifted up by liftDp when the keyboard opens (see the padding(bottom
    // = liftDp) below), so the height budget has to shrink by that same amount -- otherwise a
    // tall sheet plus the keyboard's lift can push the input right off the top of the screen
    // (or, capping the lift instead of the height, leave the input stuck behind the keyboard).
    // This does make the response area's animateContentSize() below chase a moving target while
    // the keyboard is opening/closing; that animation is kept short (150ms) specifically so that
    // catch-up stays fast enough to not read as a separate, delayed motion.
    val maxSheetHeight = (screenHeight - statusBarTop - STATUS_BAR_CLEARANCE - liftDp).coerceAtLeast(0.dp)
    val minSheetHeightPx = with(density) { MIN_SHEET_HEIGHT.toPx() }
    val maxSheetHeightPx = with(density) { maxSheetHeight.toPx() }

    val canSend = inputText.isNotBlank() && !airaUiState.isLoading

    val buttonContainerColor by animateColorAsState(
        targetValue = if (canSend) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonContainerColor"
    )
    val buttonContentColor by animateColorAsState(
        targetValue = if (canSend) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "buttonContentColor"
    )

    fun send(text: String) {
        if (text.isBlank()) return
        if (!airaUiState.canSendQuery) {
            enforceLimitUi = true
            return
        }
        if (selectedText != null) {
            onQuickAction(QuickPrompt.CustomQuestion(selectedText, text))
        } else {
            onSendMessage(text)
        }
        inputText = ""
        focusManager.clearFocus()
        onClearSelection()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(380, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = liftDp),
                verticalArrangement = Arrangement.Bottom
            ) {
                val sheetColor = MaterialTheme.colorScheme.surfaceContainerLow

                // A single content kind discriminator, distinct from PeekResponseArea's own
                // (private) state enum -- used here only to know when to snap the response
                // scroll back to the top, since switching from a long response to a totally
                // different one (or back to chips) should never carry over a stale scroll
                // offset from whatever was previously showing.
                val contentKind = when {
                    airaUiState.isLoading -> 0
                    enforceLimitUi -> 1
                    airaUiState.error != null -> 2
                    airaUiState.response != null -> 3
                    else -> 4
                }

                val sheetHeightModifier = manualHeightPx?.let { px ->
                    Modifier.height(with(density) { px.toDp() })
                } ?: Modifier.heightIn(max = maxSheetHeight)

                Surface(
                    color = sheetColor,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(sheetHeightModifier)
                        .onSizeChanged { measuredHeightPx = it.height.toFloat() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 4.dp)
                    ) {
                        // The input stays a plain, non-animated Column sibling, pinned to the
                        // sheet's bottom edge -- animateContentSize() measures its child once at
                        // the final target size and reveals it by growing the visible window
                        // from the top-left corner, it does not re-run layout at each frame. A
                        // bottom-anchored element *inside* that animated node (as this was, in an
                        // earlier version) would sit outside the still-growing window and only
                        // pop into view once the animation nearly finished. The header doesn't
                        // have that problem -- it's anchored to the *top* of the animated Box
                        // below, the same edge the size animation grows from, so it stays pinned
                        // in place throughout and can safely be an overlay on top of the
                        // response content (letting that content visually scroll behind it,
                        // fading out first) without ever moving.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Once manually resized, this must actually fill the space the
                                // user dragged open -- fill = false would leave it wrapped to
                                // content size, making a drag-to-expand on short content a no-op.
                                .weight(1f, fill = manualHeightPx != null)
                                // Kept short so that when this is driven by the keyboard
                                // opening/closing (the sheet's height ceiling shrinking or
                                // growing with it, see maxSheetHeight above) the catch-up
                                // resolves quickly instead of reading as a separate, delayed
                                // motion after the keyboard's own animation finishes.
                                .animateContentSize(animationSpec = tween(150, easing = FastOutSlowInEasing))
                        ) {
                            LaunchedEffect(contentKind) {
                                responseScrollState.scrollTo(0)
                            }

                            val headerHeightDp = with(density) { headerHeightPx.toDp() }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(responseScrollState)
                                    // Placed after verticalScroll (and after this top padding, in
                                    // scroll-content space) so the reported size is the full,
                                    // unclipped content height -- including the header-sized gap
                                    // below -- not just the currently visible viewport.
                                    .onSizeChanged { responseContentHeightPx = it.height.toFloat() }
                                    .padding(top = headerHeightDp)
                            ) {
                                PeekResponseArea(
                                    airaUiState = airaUiState,
                                    showLimitReached = enforceLimitUi,
                                    selectedText = selectedText,
                                    activeChips = activeChips,
                                    startRevealed = airaUiState.response != null &&
                                        airaUiState.response == lastRevealedResponse,
                                    onResponseRevealed = { lastRevealedResponse = it },
                                    onChipClick = { intent ->
                                        haptic.performHaptic(view)
                                        if (!airaUiState.canSendQuery) {
                                            enforceLimitUi = true
                                        } else {
                                            onQuickAction(intent)
                                            inputText = ""
                                            focusManager.clearFocus()
                                            onClearSelection()
                                        }
                                    },
                                    onGreetingSelected = { currentGreeting = it }
                                )
                            }

                            // A single continuous fade across the header's height plus the
                            // extra band below it -- not a flat solid block followed by a fade,
                            // which reads as a plain opaque header with a fade tacked on rather
                            // than content genuinely flowing/blending behind it. The extra
                            // in-between stops ease the curve so it stays stronger/more opaque
                            // through the upper portion (where the header's own icons/text sit)
                            // and only really opens up toward the bottom, without ever going
                            // flat -- still one continuous blend, just not linear.
                            if (responseScrollState.canScrollBackward) {
                                val fadeHeight = headerHeightDp + EDGE_FADE_HEIGHT
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .height(fadeHeight)
                                        .background(
                                            Brush.verticalGradient(
                                                0f to sheetColor,
                                                0.35f to sheetColor.copy(alpha = 0.9f),
                                                0.65f to sheetColor.copy(alpha = 0.55f),
                                                1f to Color.Transparent
                                            )
                                        )
                                )
                            }

                            if (responseScrollState.canScrollForward) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(EDGE_FADE_HEIGHT)
                                        .background(
                                            Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                1f to sheetColor
                                            )
                                        )
                                )
                            }

                            // The header doubles as the drag handle -- dragging it resizes the
                            // sheet, same as the old dedicated handle strip did, but without
                            // reserving its own separate row of vertical space above the title.
                            // The pill is just an overlaid affordance, not a distinct touch
                            // target.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .onSizeChanged { headerHeightPx = it.height.toFloat() }
                                    .draggable(
                                        orientation = Orientation.Vertical,
                                        state = rememberDraggableState { deltaPx ->
                                            if (hasResponseContent) {
                                                val base = manualHeightPx ?: measuredHeightPx
                                                // The header (and its overlaid drag handle) is
                                                // itself an overlay on top of the response
                                                // content below, so its height is already folded
                                                // into responseContentHeightPx via the content's
                                                // own top padding -- adding it again here would
                                                // double-count it.
                                                val naturalContentHeightPx =
                                                    responseContentHeightPx + inputRowHeightPx
                                                val maxAllowedPx = (naturalContentHeightPx + maxDragUpSlackPx)
                                                    .coerceAtMost(maxSheetHeightPx)
                                                manualHeightPx = (base - deltaPx)
                                                    .coerceIn(minSheetHeightPx, maxAllowedPx)
                                            }
                                        }
                                    )
                            ) {
                                PeekDragHandle(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 6.dp)
                                )
                                AiraPeekHeader(
                                    modifier = Modifier.padding(top = 12.dp),
                                    isLoading = airaUiState.isLoading,
                                    hasResponse = airaUiState.response != null,
                                    onDismiss = {
                                        haptic.performHaptic(view)
                                        onDismiss()
                                    },
                                    onSpeak = {
                                        haptic.performHaptic(view)
                                        contentToActUpon.let { text ->
                                            if (text.isNotBlank()) {
                                                scope.launch { voiceOutputManager.speak(text) }
                                            }
                                        }
                                    },
                                    onCopy = {
                                        haptic.performHaptic(view)
                                        onCopyResponse()
                                    }
                                )
                            }
                        }

                        AnimatedContent(
                            modifier = Modifier.onSizeChanged { inputRowHeightPx = it.height.toFloat() },
                            targetState = enforceLimitUi,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                                    animationSpec = tween(300)
                                )
                            },
                            label = "InputBarSwap"
                        ) { isLimitReached ->
                            if (!isLimitReached) {
                                InputBar(
                                    inputText = inputText,
                                    onInputChange = { inputText = it },
                                    airaUiState = airaUiState,
                                    canSend = canSend,
                                    buttonContainerColor = buttonContainerColor,
                                    buttonContentColor = buttonContentColor,
                                    horizontalPadding = horizontalPadding,
                                    focusRequester = focusRequester,
                                    onFocusChanged = { isFocused = it },
                                    onSend = {
                                        haptic.performHaptic(view)
                                        send(it)
                                    },
                                    onStop = {
                                        haptic.performHaptic(view)
                                        onStop()
                                    }
                                )
                            } else {
                                QuotaLimitBar(
                                    isPro = airaUiState.isPro
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
