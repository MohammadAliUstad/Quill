package com.yugentech.quill.reader.ui.components.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionToolbar(
    selectionInfo: SelectionInfo,
    isAiraReady: Boolean,
    onHighlight: () -> Unit,
    onAskAira: (String) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            TextButton(
                onClick = onHighlight,
                enabled = selectionInfo.locator != null
            ) {
                Text("Highlight", style = MaterialTheme.typography.labelLarge)
            }
            if (isAiraReady) {
                TextButton(onClick = { onAskAira(selectionInfo.text) }) {
                    Text("Ask Aira", style = MaterialTheme.typography.labelLarge)
                }
            }
            TextButton(onClick = { onCopy(selectionInfo.text) }) {
                Text("Copy", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
