package com.yugentech.quill.ui.more.categoryScreen.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yugentech.theme.tokens.AppConstants.EMPTY
import com.yugentech.theme.tokens.AppConstants.FAVOURITES
import com.yugentech.theme.tokens.AppConstants.SHELF

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(EMPTY) }

    val isReserved =
        text.equals(SHELF, ignoreCase = true) || text.equals(FAVOURITES, ignoreCase = true)
    val isBlank = text.isBlank()
    val isError = isReserved || (text.isNotEmpty() && isBlank)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Collection") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Category Name") },
                isError = isError,
                supportingText = {
                    if (isReserved) Text("Name reserved for system")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = !isError && !isBlank
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) { Text("Cancel") }
        }
    )
}