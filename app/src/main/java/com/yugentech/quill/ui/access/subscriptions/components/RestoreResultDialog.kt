package com.yugentech.quill.ui.access.subscriptions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.R
import com.yugentech.quill.billing.viewmodel.RestoreResult
import com.yugentech.quill.ui.common.components.QuillDialog

/** Why Restore didn't bring Pro back. Only an OK button: there's nothing to choose. */
@Composable
fun RestoreResultDialog(result: RestoreResult, onDismiss: () -> Unit) {
    val notFound = result == RestoreResult.NotFound
    QuillDialog(
        icon = if (notFound) Icons.Outlined.Info else Icons.Outlined.ErrorOutline,
        title = stringResource(if (notFound) R.string.restore_not_found_title else R.string.restore_failed_title),
        message = stringResource(
            when (result) {
                RestoreResult.NotFound -> R.string.restore_not_found_message
                RestoreResult.ConnectionFailed -> R.string.billing_connect_failed
                RestoreResult.NeedsAccount -> R.string.guest_subscribe_needs_account
                RestoreResult.NotSignedIn -> R.string.billing_login_restore
            }
        ),
        primaryLabel = stringResource(R.string.ok),
        onPrimary = onDismiss,
        onDismiss = onDismiss
    )
}
