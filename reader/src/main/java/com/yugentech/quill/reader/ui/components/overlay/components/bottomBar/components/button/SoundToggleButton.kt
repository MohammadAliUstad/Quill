package com.yugentech.quill.reader.ui.components.overlay.components.bottomBar.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.model.BackgroundSound

@Composable
fun SoundToggleButton(
    currentSound: BackgroundSound,
    lastSelectedSound: BackgroundSound,
    onClick: () -> Unit
) {
    val isPlaying = currentSound != BackgroundSound.NONE
    val soundLabel = if (isPlaying) currentSound.displayName else lastSelectedSound.displayName

    ElevatedButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer 
                           else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer 
                         else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.VolumeUp else Icons.Rounded.MusicNote,
            contentDescription = "Toggle Sound",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp)
        )
        Text(
            text = soundLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
