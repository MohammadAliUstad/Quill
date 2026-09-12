package com.yugentech.quill.reader.ui.components.soundSheet

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.sound.model.BackgroundSound
import com.yugentech.quill.reader.ui.components.settingsSheet.components.SettingsSwitchItem
import com.yugentech.theme.service.HapticService
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundSelectionSheet(
    selectedSound: BackgroundSound,
    isSoundOn: Boolean,
    autoPlayEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSoundToggle: (BackgroundSound) -> Unit,
    onAutoPlayChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
            Column {
                Text(
                    text = "Ambient Sounds",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose a soothing ambience to stay immersed in your reads.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSwitchItem(
                title = "Turn On Sounds",
                subtitle = "Enable to preview and pick a sound below",
                checked = isSoundOn,
                onCheckedChange = onSoundEnabledChange
            )

            SettingsSwitchItem(
                title = "Auto-play on Open",
                subtitle = "Start sound automatically when opening a book",
                checked = autoPlayEnabled,
                enabled = isSoundOn,
                onCheckedChange = onAutoPlayChange
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Sounds are only pickable while ambient sound is on; while it's off the grid stays
            // visible (dimmed) so the user can still see which sound will play when they turn it on.
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
                                isSelected = selectedSound == option.sound,
                                enabled = isSoundOn,
                                onClick = { onSoundToggle(option.sound) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SoundToggleCard(
    option: SoundOption,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = koinInject<HapticService>()
    val view = LocalView.current
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected && enabled) 1.3f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )

    ToggleButton(
        checked = isSelected,
        onCheckedChange = {
            haptic.performHaptic(view)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
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
