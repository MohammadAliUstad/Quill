package com.yugentech.quill.reader.ui.components.overlay.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.reader.R
import androidx.compose.ui.res.stringResource

@Composable
fun AskAiraButton(
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    ElevatedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
            onClick()
        },
        shape = CircleShape,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = Modifier
            .padding(end = 8.dp)
            .height(52.dp)
            .widthIn(min = 140.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.reader_ask_aira),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(22.dp)
            )
            Text(
                text = stringResource(R.string.reader_ask_aira),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
