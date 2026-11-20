package com.yugentech.quill.ui.info.storage.components

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
fun DeleteBookDialog(
    bookTitle: String,
    // An imported book is removed from the library with its file, so the dialog says so
    isImported: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                if (isImported) stringResource(R.string.remove_library_title)
                else stringResource(R.string.delete_download_title)
            )
        },
        text = {
            Text(
                if (isImported) stringResource(R.string.delete_imported_body, bookTitle)
                else stringResource(R.string.delete_download_body, bookTitle)
            )
        },
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
                Text(
                    if (isImported) stringResource(R.string.remove_completely)
                    else stringResource(R.string.delete)
                )
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