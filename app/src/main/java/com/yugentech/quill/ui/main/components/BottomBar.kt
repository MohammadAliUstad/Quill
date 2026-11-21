package com.yugentech.quill.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.graphics.shapes.Morph
import com.yugentech.quill.ui.shared.shapes.BloomShapes
import com.yugentech.quill.ui.shared.shapes.MorphShape
import kotlin.random.Random

/** Height of the dock. */
private val DockHeight: Dp = 64.dp

/** Size of the + button beside the dock: a little smaller than the dock, centred on it. */
private val ImportButtonSize: Dp = 56.dp

/** Height of each tab's capsule inside the dock. */
private val TabHeight = 48.dp

/** How long the + button holds each shape before morphing into the next, at rest and importing. */
private const val ShapeHoldMillis = 1_600L
private const val ShapeHoldImportingMillis = 300L

/** One full turn of the + button's shape, at rest and while books import. */
private const val TurnMillis = 12_000
private const val TurnImportingMillis = 2_400

/** How far the + button shrinks while pressed. */
private const val PressedScale = 0.9f

/**
 * A floating pill dock, as in Sessions and Hajime: each tab is just its animated icon, and the
 * selected one grows into a filled capsule that also spells out its name.
 *
 * Beside it, apart, a round + button imports the reader's own books. It's an action, not a place,
 * so it sits outside the pill and never takes a selected state the way a tab would. While books
 * are importing it shows a spinner instead, and can't start a second import.
 */
@Composable
fun BottomBar(
    tabs: List<QuillTab>,
    currentTab: QuillTab,
    onTabSelected: (QuillTab) -> Unit,
    isImporting: Boolean,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.height(DockHeight),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                tabs.forEach { tab ->
                    DockTab(
                        tab = tab,
                        isSelected = tab == currentTab,
                        onClick = {
                            if (tab != currentTab) {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onTabSelected(tab)
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        ImportButton(
            isImporting = isImporting,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onImportClick()
            }
        )
    }
}

@Composable
private fun DockTab(
    tab: QuillTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer else Color.Transparent,
        animationSpec = motion.fastEffectsSpec(),
        label = "dockTabContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = motion.fastEffectsSpec(),
        label = "dockTabContent"
    )
    val horizontalPad by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 12.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "dockTabPad"
    )
    val iconRes = when (tab) {
        QuillTab.Library -> R.drawable.anim_library_enter
        QuillTab.Discover -> R.drawable.avd_explore_enter
        QuillTab.Settings -> R.drawable.anim_more_enter
    }
    val title = stringResource(tab.title)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(TabHeight)
            .clip(CircleShape)
            .background(container)
            // No ripple: the capsule growing is the feedback
            .selectable(
                selected = isSelected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { contentDescription = title }
            .padding(horizontal = horizontalPad)
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            AnimatedNavIcon(selected = isSelected, avdRes = iconRes)
        }
        // The name slides out beside the icon on the selected tab only
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(motion.fastEffectsSpec()) +
                expandHorizontally(motion.fastSpatialSpec(), expandFrom = Alignment.Start),
            exit = fadeOut(motion.fastEffectsSpec()) +
                shrinkHorizontally(motion.fastSpatialSpec(), shrinkTowards = Alignment.Start)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * The + beside the dock, a little smaller than it, filled in the theme's primary colour. It's never
 * still: it holds one of [BloomShapes] for a moment, morphs straight into another (never the same
 * one twice in a row, never back through a circle), and turns slowly the whole time. While books
 * are importing it morphs and turns faster, which is its progress indicator, and can't be pressed
 * again. Pressed, it shrinks a touch under the ripple.
 */
@Composable
private fun ImportButton(
    isImporting: Boolean,
    onClick: () -> Unit
) {
    val description = stringResource(R.string.import_books)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentlyImporting by rememberUpdatedState(isImporting)

    // From one shape to the next. When a morph lands, the next one starts from where it ended, so
    // the outline carries on without a jump
    var fromShape by remember { mutableIntStateOf(Random.nextInt(BloomShapes.size)) }
    var toShape by remember { mutableIntStateOf(otherShapeThan(fromShape)) }
    val morphProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(if (currentlyImporting) ShapeHoldImportingMillis else ShapeHoldMillis)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = if (currentlyImporting) Spring.StiffnessMediumLow else Spring.StiffnessLow
                )
            )
            fromShape = toShape
            toShape = otherShapeThan(fromShape)
            morphProgress.snapTo(0f)
        }
    }
    val morph = remember(fromShape, toShape) { Morph(BloomShapes[fromShape], BloomShapes[toShape]) }

    // A steady turn, quicker while importing. Each change of pace carries on from the current
    // angle, so the shape never jumps
    val turn = remember { Animatable(0f) }
    LaunchedEffect(isImporting) {
        turn.snapTo(turn.value % 360f)
        val millis = if (isImporting) TurnImportingMillis else TurnMillis
        while (true) {
            turn.animateTo(
                targetValue = turn.value + 360f,
                animationSpec = tween(durationMillis = millis, easing = LinearEasing)
            )
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) PressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "importPressScale"
    )

    val shape = MorphShape(morph, morphProgress.value, turn.value)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ImportButtonSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                enabled = !isImporting,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description }
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/** Any of [BloomShapes] but the one at [index]. */
private fun otherShapeThan(index: Int): Int {
    val next = Random.nextInt(BloomShapes.size - 1)
    return if (next >= index) next + 1 else next
}

@Composable
private fun AnimatedNavIcon(
    selected: Boolean,
    avdRes: Int
) {
    val image = AnimatedImageVector.animatedVectorResource(avdRes)
    Icon(
        painter = rememberAnimatedVectorPainter(
            animatedImageVector = image,
            atEnd = selected
        ),
        // The tab itself carries the name
        contentDescription = null
    )
}
