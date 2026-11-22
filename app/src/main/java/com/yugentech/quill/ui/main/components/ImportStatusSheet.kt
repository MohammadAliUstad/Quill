package com.yugentech.quill.ui.main.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.background
import com.yugentech.theme.SheetTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.yugentech.quill.library.importer.ImportResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStatusSheet(
    results: List<ImportResult>,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val successCount = results.count { it is ImportResult.Success }
    val failureCount = results.count { it is ImportResult.Failure }
    val existingCount = results.count { it is ImportResult.AlreadyExists }
    // Every book picked was already there: a gentle note rather than an import report
    val onlyExisting = existingCount == results.size

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    SheetTheme {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (onlyExisting) {
                        stringResource(R.string.import_already_title)
                    } else {
                        stringResource(R.string.import_complete)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                val single = results.singleOrNull()
                Text(
                    text = if (single is ImportResult.AlreadyExists) {
                        stringResource(R.string.import_already_single, single.title)
                    } else {
                        listOfNotNull(
                            if (successCount > 0) pluralStringResource(R.plurals.import_books_succeeded, successCount, successCount) else null,
                            if (existingCount > 0) pluralStringResource(R.plurals.import_books_already, existingCount, existingCount) else null,
                            if (failureCount > 0) pluralStringResource(R.plurals.import_books_failed, failureCount, failureCount) else null
                        ).reduceOrNull { first, second -> stringResource(R.string.list_pair, first, second) }.orEmpty()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (results.size > 1) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(results) { result ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                when (result) {
                                    is ImportResult.Success -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = result.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    is ImportResult.AlreadyExists -> {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = result.title,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = stringResource(R.string.import_already_row),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    is ImportResult.Failure -> {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = result.fileName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = result.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    // As the Save button of Sessions' sound sheet
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text(
                        text = if (successCount > 0) stringResource(R.string.go_to_library) else stringResource(R.string.close),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}