package com.yugentech.quill.ui.book.airaChat.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@Composable
fun AiraResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.aira_reset_title)) },
        text = { Text(stringResource(R.string.aira_reset_body)) },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                onDismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}