package com.yugentech.quill.ui.book.airaChat.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** An error across the top of Aira's chat; tapping it dismisses it. */
@Composable
fun StatusBanner(
    error: String,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val textColor = MaterialTheme.colorScheme.onErrorContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onDismiss()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            "✕",
            style = MaterialTheme.typography.labelMedium,
            color = textColor.copy(alpha = 0.6f)
        )
    }
}
