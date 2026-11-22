package com.yugentech.quill.ui.tabs.settingsScreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.yugentech.quill.ui.common.components.BloomShapes
import com.yugentech.quill.ui.common.components.MorphShape
import com.yugentech.theme.tokens.dimensions.AppAnimations
import com.yugentech.theme.tokens.spacing
import kotlin.math.roundToInt
import kotlin.random.Random

// Size of each avatar in the avatar sheet's grid, three to a row.
internal val AvatarOptionSize = 112.dp

// How far the selected avatar's shape turns as it blooms, in degrees.
private const val SelectedSpin = 40f

// The avatar name's font weight, unselected and selected; it thickens smoothly between the two.
private const val LabelWeightRest = 400
private const val LabelWeightSelected = 600

// Round shapes that fill the circle about as fully as the cookie does, so the figure cropped to
// any of them keeps its face
private val SelectedShapes = BloomShapes

// Unselected avatars sit on a plain circle. Selecting one blooms its circle into one of
// [SelectedShapes], picked at random each time (never the same one twice in a row), in
// primaryContainer with a small turn; the figure stays upright and is cropped to the same
// shape, so it sits inside it rather than on top of it.
@Composable
fun AvatarOption(
    avatar: Avatar,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    // The last shape picked, kept so a deselected avatar eases back from the shape it showed
    val lastShape = remember { intArrayOf(Random.nextInt(SelectedShapes.size)) }
    val shapeIndex = remember(isSelected) {
        if (isSelected) {
            val next = Random.nextInt(SelectedShapes.size - 1)
            (if (next >= lastShape[0]) next + 1 else next).also { lastShape[0] = it }
        } else {
            lastShape[0]
        }
    }
    val morph = remember(shapeIndex) { Morph(MaterialShapes.Circle, SelectedShapes[shapeIndex]) }

    // Settles without overshooting: the shape eases in and the name thickens with it
    val progress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "avatarShapeMorph"
    )
    val rotation by animateFloatAsState(
        targetValue = if (isSelected) SelectedSpin else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "avatarShapeSpin"
    )
    val backdrop by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        label = "avatarShapeColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "avatarLabelColor"
    )
    val shape = MorphShape(morph, progress, rotation)

    Column(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
    ) {
        Box(
            modifier = Modifier
                .size(AvatarOptionSize)
                .clip(shape)
                .background(backdrop)
                // No haptic here: the sheet confirms the pick with its own, and two read as a buzz
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            AvatarImage(
                avatarId = avatar.id,
                size = AvatarOptionSize,
                imageSize = 134.dp,
                imageOffsetY = 6.dp
            )
        }

        // Rides the shape's spring, so the name thickens in step with it. Rounded to steps of 10
        // so only a handful of weights are ever built (and each is cached once it has been).
        val labelWeight = (LabelWeightRest + (LabelWeightSelected - LabelWeightRest) * progress)
            .roundToInt()
            .let { (it + 5) / 10 * 10 }
        val baseFamily = MaterialTheme.typography.labelMedium.fontFamily
        val labelFamily = remember(baseFamily, labelWeight) { fontFamilyAtWeight(baseFamily, labelWeight) }

        Text(
            text = stringResource(avatar.name),
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            fontFamily = labelFamily,
            fontWeight = FontWeight(labelWeight),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee(
                animationMode = MarqueeAnimationMode.Immediately,
                repeatDelayMillis = AppAnimations.Durations.RepeatDelay,
                initialDelayMillis = AppAnimations.Durations.Delay,
                velocity = AppAnimations.Motion.Velocity
            )
        )
    }
}

// The theme's fonts are variable, but its family only lists five fixed weights, so a weight in
// between would snap to the nearest. This builds the same font file at exactly [weight] instead,
// so the weight can move smoothly. Falls back to [family] as it is if it isn't a resource font.
@OptIn(ExperimentalTextApi::class)
private fun fontFamilyAtWeight(family: FontFamily?, weight: Int): FontFamily? {
    val font = (family as? FontListFontFamily)?.fonts?.firstOrNull() as? ResourceFont ?: return family
    return FontFamily(
        Font(
            resId = font.resId,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight))
        )
    )
}
