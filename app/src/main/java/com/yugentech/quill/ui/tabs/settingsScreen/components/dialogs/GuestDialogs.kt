package com.yugentech.quill.ui.tabs.settingsScreen.components.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.R
import com.yugentech.quill.ui.common.components.DialogAction
import com.yugentech.quill.ui.common.components.QuillDialog

/**
 * Leaving guest mode deletes everything the guest added, so this says so plainly and offers
 * to export it first (the file can be imported later, including into an account). Deleting is
 * the quiet option, so it doesn't outweigh exporting.
 */
@Composable
fun ExitGuestDialog(
    onConfirm: () -> Unit,
    onExportAndExit: () -> Unit,
    onDismiss: () -> Unit
) {
    QuillDialog(
        icon = Icons.Outlined.DeleteForever,
        title = stringResource(R.string.guest_exit_title),
        message = stringResource(R.string.guest_exit_message),
        primaryLabel = stringResource(R.string.guest_export_and_exit),
        onPrimary = onExportAndExit,
        onDismiss = onDismiss,
        secondaryActions = listOf(
            DialogAction(stringResource(R.string.cancel), onDismiss, MaterialTheme.colorScheme.onSurfaceVariant),
            DialogAction(stringResource(R.string.guest_exit_confirm), onConfirm, MaterialTheme.colorScheme.error)
        ),
        secondaryActionsInRow = true
    )
}

/** How a guest's export or import went. Only an OK button: there's nothing to choose. */
@Composable
fun BackupResultDialog(
    success: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    QuillDialog(
        icon = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
        title = title,
        message = message,
        primaryLabel = stringResource(R.string.ok),
        onPrimary = onDismiss,
        onDismiss = onDismiss,
        destructive = !success
    )
}
