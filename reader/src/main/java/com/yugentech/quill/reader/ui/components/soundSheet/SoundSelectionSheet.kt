package com.yugentech.quill.reader.ui.components.soundSheet

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.model.BackgroundSound
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundSelectionSheet(
    activeSound: BackgroundSound,
    volume: Float,
    onSoundToggle: (BackgroundSound) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    val cornerRadius by animateDpAsState(
        targetValue = if (sheetState.targetValue == SheetValue.Expanded) 0.dp else 28.dp,
        label = "sheetCornerRadius"
    )

    val options = listOf(
        SoundOption("Forest", BackgroundSound.FOREST, Icons.Rounded.Forest),
        SoundOption("Rain", BackgroundSound.RAIN, Icons.Rounded.WaterDrop),
        SoundOption("Brown Noise", BackgroundSound.BROWN_NOISE, Icons.Rounded.BlurOn),
        SoundOption("Fireplace", BackgroundSound.FIREPLACE, Icons.Rounded.LocalFireDepartment),
        SoundOption("Library", BackgroundSound.LIBRARY, Icons.AutoMirrored.Rounded.LibraryBooks),
        SoundOption("Riverside", BackgroundSound.RIVERSIDE, Icons.Rounded.Water)
    )

    val noneOption = SoundOption("None", BackgroundSound.NONE, Icons.Rounded.Block)

    val volumeSliderInteractionSource = remember { MutableInteractionSource() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
        ) {
            Text(
                text = "Ambient Sounds",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
            ) {
                options.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
                    ) {
                        rowOptions.forEach { option ->
                            SoundToggleCard(
                                option = option,
                                isSelected = activeSound == option.sound,
                                onClick = { onSoundToggle(option.sound) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                SoundToggleCard(
                    option = noneOption,
                    isSelected = activeSound == noneOption.sound,
                    onClick = { onSoundToggle(noneOption.sound) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val volumeIcon = when {
                    volume == 0f -> Icons.AutoMirrored.Rounded.VolumeOff
                    volume < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                    else -> Icons.AutoMirrored.Rounded.VolumeUp
                }

                Crossfade(targetState = volumeIcon, label = "volume_icon_fade") { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
                
                androidx.compose.material3.Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    interactionSource = volumeSliderInteractionSource,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        thumbColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    thumb = { state ->
                        SliderDefaults.Thumb(
                            interactionSource = volumeSliderInteractionSource,
                            sliderState = state,
                            thumbSize = DpSize(4.dp, 44.dp),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    },
                    track = { state ->
                        SliderDefaults.Track(
                            sliderState = state,
                            modifier = Modifier.height(28.dp),
                            thumbTrackGapSize = 6.dp,
                            trackCornerSize = 14.dp,
                            drawStopIndicator = null
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SoundToggleCard(
    option: SoundOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )

    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = modifier,
        shapes = ToggleButtonShapes(
            shape = RoundedCornerShape(MaterialTheme.corners.medium),
            pressedShape = RoundedCornerShape(MaterialTheme.corners.small),
            checkedShape = CircleShape
        ),
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.s))

            Text(
                text = option.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private data class SoundOption(
    val label: String,
    val sound: BackgroundSound,
    val icon: ImageVector
)
