package com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.sound.model.BackgroundSound
import com.yugentech.theme.service.HapticService
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundToggleButton(
    currentSound: BackgroundSound,
    lastSelectedSound: BackgroundSound,
    onClick: () -> Unit
) {
    val haptic = koinInject<HapticService>()
    val view = LocalView.current
    val isPlaying = currentSound != BackgroundSound.NONE
    val soundLabel = if (isPlaying) currentSound.displayName else lastSelectedSound.displayName

    ToggleButton(
        checked = isPlaying,
        onCheckedChange = {
            haptic.performHaptic(view)
            onClick()
        },
        shapes = ToggleButtonShapes(
            shape = CircleShape,
            pressedShape = RoundedCornerShape(12.dp),
            checkedShape = RoundedCornerShape(8.dp)
        ),
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPlaying) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.MusicNote,
                contentDescription = "Toggle Sound",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(22.dp)
            )
            Text(
                text = soundLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
