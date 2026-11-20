package com.yugentech.quill.reader.ui.components.aira

import com.yugentech.theme.SheetTheme
import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.imeAnimationTarget
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.yugentech.quill.aira.chat.quickChat.prompt.QuickPrompt
import com.yugentech.quill.reader.viewmodel.quick.QuickUiState
import com.yugentech.quill.reader.ui.components.aira.components.AiraPeekHeader
import com.yugentech.quill.reader.ui.components.aira.components.InputBar
import com.yugentech.quill.reader.ui.components.aira.components.PeekDragHandle
import com.yugentech.quill.reader.ui.components.aira.components.PeekResponseArea
import com.yugentech.quill.reader.ui.components.aira.components.QuotaLimitBar
import com.yugentech.quill.reader.ui.components.aira.components.resolveChips
import com.yugentech.theme.tokens.dimensions.AppAnimations
import org.readium.r2.shared.publication.Locator

private val STATUS_BAR_CLEARANCE = 16.dp
private val EDGE_FADE_HEIGHT = 20.dp
private val MIN_SHEET_HEIGHT = 220.dp

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
    val haptic = LocalHapticFeedback.current
    val fadeThrough = AppAnimations.Transitions.rememberExpressiveFadeThrough()
    var inputText by remember { mutableStateOf("") }

    // Closing hides the bar and clears the response/selection upstream in the same moment, but
    // the exit slide takes 300ms -- rendering the live values would swap a long response for the
    // chips mid-slide, collapsing the sheet and sliding the chips back up into view before it
    // finishes leaving. Hold whatever was last shown while visible so it slides out unchanged.
    val shownUiState = rememberHeldWhileHidden(isVisible, airaUiState)
    val shownSelectedText = rememberHeldWhileHidden(isVisible, selectedText)
    val shownSelectedTextLocator = rememberHeldWhileHidden(isVisible, selectedTextLocator)
    val shownChapterIndex = rememberHeldWhileHidden(isVisible, currentChapterIndex)

    // Tapping outside closes the peek bar via AnimatedVisibility, which disposes its content --
    // including PeekResponseArea's TypewriterText and whatever reveal progress it had. Remembered
    // here, above that AnimatedVisibility, so it survives the close/reopen and a message that
    // already finished typing out once shows up instantly instead of replaying from scratch.
    var lastRevealedResponse by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var isFocused by remember { mutableStateOf(false) }

    val activeChips = remember(shownSelectedText, shownChapterIndex, shownSelectedTextLocator) {
        resolveChips(shownSelectedText, shownChapterIndex, shownSelectedTextLocator)
    }

    var enforceLimitUi by remember { mutableStateOf(!shownUiState.canSendQuery) }

    // null = automatic, content-driven height. Once the user drags the handle, this becomes
    // an explicit override for the reply on screen. It's cleared when the next question is
    // sent (below) and when the bar closes, so a short answer after a long, hand-resized one
    // shrinks to fit straight away instead of keeping the old height until the handle is touched.
    var manualHeightPx by remember { mutableStateOf<Float?>(null) }
    var measuredHeightPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    // Only a real result is worth resizing around -- the empty chips/greeting screen has
    // nothing whose size the user would want to adjust.
    val hasResponseContent = shownUiState.response != null || shownUiState.imagePath != null

    // Natural (unclamped) heights of each sibling, tracked independently of whatever height the
    // user has manually forced the sheet to -- these feed the drag handle's upward limit so it
    // never opens past the content's own natural height.
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
    // Matches the sheet Column's windowInsetsPadding(navigationBars) + padding(bottom = 4.dp).
    val sheetBottomChromePx = with(density) {
        WindowInsets.navigationBars.getBottom(this) + 4.dp.toPx()
    }

    val visibleState = remember { MutableTransitionState(isVisible) }
    visibleState.targetState = isVisible

    LaunchedEffect(isVisible) {
        if (isVisible) enforceLimitUi = !shownUiState.canSendQuery
    }

    // Reset only once the exit slide has fully finished -- clearing manualHeightPx while still
    // sliding out would snap a hand-resized sheet back to its automatic height mid-animation.
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) {
            inputText = ""
            manualHeightPx = null
        }
    }

    // A new question (typed, or a quick action) goes back to the automatic height: the sheet
    // eases down to the thinking dots and then grows to fit the new answer, through the
    // response area's size animation, rather than holding a height dragged for the last reply.
    LaunchedEffect(shownUiState.isLoading) {
        if (shownUiState.isLoading) manualHeightPx = null
    }

    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) { if (!isImeVisible) focusManager.clearFocus() }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liftDp = (imeBottom - navBottom).coerceAtLeast(0.dp)
    val kbFraction = (imeBottom / 300.dp).coerceIn(0f, 1f)
    // The keyboard is opening or closing: it isn't yet where it's heading
    val imeTarget = WindowInsets.imeAnimationTarget.asPaddingValues().calculateBottomPadding()
    val isImeMoving = imeBottom != imeTarget
    // Narrower while the keyboard is closed, widening to nearly the full width as it opens
    val horizontalPadding = lerp(24.dp, 8.dp, kbFraction)

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    // The whole sheet gets shifted up by liftDp when the keyboard opens (see the padding(bottom
    // = liftDp) below), so the height budget has to shrink by that same amount -- otherwise a
    // tall sheet plus the keyboard's lift can push the input right off the top of the screen
    // (or, capping the lift instead of the height, leave the input stuck behind the keyboard).
    // The response area's size animation below is paused while the keyboard moves, so the
    // sheet follows this ceiling exactly instead of chasing it.
    val maxSheetHeight = (screenHeight - statusBarTop - STATUS_BAR_CLEARANCE - liftDp).coerceAtLeast(0.dp)
    val minSheetHeightPx = with(density) { MIN_SHEET_HEIGHT.toPx() }
    val maxSheetHeightPx = with(density) { maxSheetHeight.toPx() }
    val liftPx = with(density) { liftDp.toPx() }

    // A dragged height is kept as the height with the keyboard closed. While the keyboard is
    // open the sheet shrinks by its lift instead of sliding up whole, so its top stays where
    // the user left it, as an automatic-height sheet's does. Only once it's down to the
    // minimum (or its own height, if smaller) does the rest of the lift push it up.
    fun shownHeightPx(draggedPx: Float): Float =
        (draggedPx - liftPx)
            .coerceAtLeast(minOf(draggedPx, minSheetHeightPx))
            .coerceAtMost(maxSheetHeightPx)

    val canSend = inputText.isNotBlank() && !shownUiState.isLoading

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
        if (!shownUiState.canSendQuery) {
            enforceLimitUi = true
            return
        }
        if (shownSelectedText != null) {
            onQuickAction(QuickPrompt.CustomQuestion(shownSelectedText, text))
        } else {
            onSendMessage(text)
        }
        inputText = ""
        focusManager.clearFocus()
        onClearSelection()
    }

    // Black under AMOLED, like the reader's other sheets
    SheetTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
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
                        shownUiState.isLoading -> 0
                        enforceLimitUi -> 1
                        shownUiState.error != null -> 2
                        shownUiState.response != null -> 3
                        else -> 4
                    }

                    val sheetHeightModifier = manualHeightPx?.let { px ->
                        Modifier.height(with(density) { shownHeightPx(px).toDp() })
                    } ?: Modifier.heightIn(max = maxSheetHeight)

                    Surface(
                        color = sheetColor,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        // No tonal tint: under AMOLED it would lift the black sheet to a grey
                        tonalElevation = 0.dp,
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
                            // The reply runs on under the input row, so it shows around the pill
                            // instead of stopping at a solid band: this Box draws [inputRowHeight]
                            // further down than the space it takes, and its content has that much
                            // more room at the end. Measured from outside, its height is what it
                            // was, so the sheet's size, the drag limits and the input's place don't
                            // change, and a reply scrolled to its end still stops just above the pill.
                            val inputRowHeight = with(density) { inputRowHeightPx.toDp() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .layout { measurable, constraints ->
                                        val extra = inputRowHeightPx.roundToInt()
                                        val placeable = measurable.measure(
                                            constraints.copy(
                                                minHeight = constraints.minHeight + extra,
                                                maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight + extra
                                                else constraints.maxHeight
                                            )
                                        )
                                        layout(placeable.width, (placeable.height - extra).coerceAtLeast(0)) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                    // Once manually resized, this must actually fill the space the
                                    // user dragged open -- fill = false would leave it wrapped to
                                    // content size, making a drag-to-expand on short content a no-op.
                                    .weight(1f, fill = manualHeightPx != null)
                                    // Off while the keyboard moves: a sheet at its full height is
                                    // held there by maxSheetHeight, which follows the keyboard. As
                                    // the keyboard closed, the sheet's bottom dropped with it at
                                    // once while this eased the height up behind it, so the top
                                    // dipped and came back. Without it the height tracks the
                                    // keyboard frame by frame. A spring rather than a tween: a
                                    // reply growing line by line retargets this every frame, and a
                                    // tween restarts its easing from zero speed on each retarget (a
                                    // visible stutter) while a spring keeps its velocity.
                                    // Kept on while a new question is loading, though: sending a
                                    // typed question closes the keyboard in the same moment the
                                    // reply gives way to the thinking dots, and with this off the
                                    // response area snapped shut. Shrinking to the dots can't dip
                                    // the way a tall reply could, so it eases down instead.
                                    .then(
                                        if (isImeMoving && !shownUiState.isLoading) Modifier
                                        else Modifier.animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    )
                            ) {
                                LaunchedEffect(contentKind) {
                                    responseScrollState.scrollTo(0)
                                }

                                val headerHeightDp = with(density) { headerHeightPx.toDp() }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(responseScrollState)
                                        // Room to scroll the reply's end clear of the input row. Outside
                                        // onSizeChanged, so the content height it reports stays the
                                        // reply's own, as the drag limit expects.
                                        .padding(bottom = inputRowHeight)
                                        // Placed after verticalScroll (and after this top padding, in
                                        // scroll-content space) so the reported size is the full,
                                        // unclipped content height -- including the header-sized gap
                                        // below -- not just the currently visible viewport.
                                        .onSizeChanged { responseContentHeightPx = it.height.toFloat() }
                                        .padding(top = headerHeightDp)
                                ) {
                                    PeekResponseArea(
                                        airaUiState = shownUiState,
                                        showLimitReached = enforceLimitUi,
                                        selectedText = shownSelectedText,
                                        activeChips = activeChips,
                                        startRevealed = shownUiState.response != null &&
                                            shownUiState.response == lastRevealedResponse,
                                        onResponseRevealed = { lastRevealedResponse = it },
                                        onChipClick = { intent ->
                                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            if (!shownUiState.canSendQuery) {
                                                enforceLimitUi = true
                                            } else {
                                                onQuickAction(intent)
                                                inputText = ""
                                                focusManager.clearFocus()
                                                onClearSelection()
                                            }
                                        }
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

                                // Behind the input row, as behind the Aira chat's input bar (the same
                                // stops): the reply fades out from just above the pill to the sheet's
                                // own colour at its bottom. Always there, like the chat's, so it shows
                                // the moment a reply runs under the pill; over the empty sheet it's
                                // the sheet's colour on itself, so it can't be seen.
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(EDGE_FADE_HEIGHT + inputRowHeight)
                                        .background(
                                            Brush.verticalGradient(
                                                0.00f to Color.Transparent,
                                                0.15f to sheetColor.copy(alpha = 0.1f),
                                                0.35f to sheetColor.copy(alpha = 0.4f),
                                                0.55f to sheetColor.copy(alpha = 0.7f),
                                                0.75f to sheetColor.copy(alpha = 0.9f),
                                                1.00f to sheetColor
                                            )
                                        )
                                )

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
                                                    // The height on screen now, which with the
                                                    // keyboard open is the shrunk one
                                                    val base = manualHeightPx?.let(::shownHeightPx) ?: measuredHeightPx
                                                    // The header (and its overlaid drag handle) is
                                                    // itself an overlay on top of the response
                                                    // content below, so its height is already folded
                                                    // into responseContentHeightPx via the content's
                                                    // own top padding -- adding it again here would
                                                    // double-count it. The sheet's own bottom chrome
                                                    // (nav bar inset + 4dp padding on the Column) does
                                                    // count though -- leaving it out capped the drag
                                                    // short of the natural height, so a shrunk sheet
                                                    // could never be dragged fully open again and its
                                                    // response stayed scrollable by that difference.
                                                    val naturalContentHeightPx =
                                                        responseContentHeightPx + inputRowHeightPx + sheetBottomChromePx
                                                    // Never below the minimum, or coerceIn would throw
                                                    // for a reply shorter than MIN_SHEET_HEIGHT.
                                                    val maxAllowedPx = naturalContentHeightPx
                                                        .coerceAtMost(maxSheetHeightPx)
                                                        .coerceAtLeast(minSheetHeightPx)
                                                    val shownPx = (base - deltaPx)
                                                        .coerceIn(minSheetHeightPx, maxAllowedPx)
                                                    // Kept as the keyboard-closed height it shows as
                                                    manualHeightPx = shownPx + liftPx
                                                }
                                            }
                                        )
                                ) {
                                    PeekDragHandle(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 14.dp)
                                    )
                                    AiraPeekHeader(
                                        // Moved down with the handle, so the gap under it is unchanged
                                        modifier = Modifier.padding(top = 20.dp),
                                        onDismiss = {
                                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            onDismiss()
                                        }
                                    )
                                }
                            }

                            AnimatedContent(
                                modifier = Modifier.onSizeChanged { inputRowHeightPx = it.height.toFloat() },
                                targetState = enforceLimitUi,
                                transitionSpec = { fadeThrough() },
                                label = "InputBarSwap"
                            ) { isLimitReached ->
                                if (!isLimitReached) {
                                    InputBar(
                                        inputText = inputText,
                                        onInputChange = { inputText = it },
                                        airaUiState = shownUiState,
                                        canSend = canSend,
                                        buttonContainerColor = buttonContainerColor,
                                        buttonContentColor = buttonContentColor,
                                        horizontalPadding = horizontalPadding,
                                        focusRequester = focusRequester,
                                        onFocusChanged = { isFocused = it },
                                        onSend = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            send(it)
                                        },
                                        onStop = {
                                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            onStop()
                                        }
                                    )
                                } else {
                                    QuotaLimitBar(
                                        isPro = shownUiState.isPro
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class Held<T>(var value: T)

// Tracks [value] while visible and keeps returning the last visible one once hidden. A plain
// holder rather than state: a change to [value] already recomposes the caller, so nothing needs
// to observe the write.
@Composable
private fun <T> rememberHeldWhileHidden(isVisible: Boolean, value: T): T {
    val held = remember { Held(value) }
    if (isVisible) held.value = value
    return held.value
}
